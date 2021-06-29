package com.decompiler.state;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;

import com.decompiler.apiunreleased.ClassFileSource2;
import com.decompiler.apiunreleased.JarContent;
import com.decompiler.bytecode.analysis.parse.utils.Pair;
import com.decompiler.bytecode.analysis.types.ClassNameUtils;
import com.decompiler.bytecode.analysis.types.JavaRefTypeInstance;
import com.decompiler.bytecode.analysis.types.JavaTypeInstance;
import com.decompiler.entities.ClassFile;
import com.decompiler.mapping.NullMapping;
import com.decompiler.mapping.ObfuscationMapping;
import com.decompiler.util.AnalysisType;
import com.decompiler.util.CannotLoadClassException;
import com.decompiler.util.DecompilerComment;
import com.decompiler.util.MiscConstants;
import com.decompiler.util.bytestream.BaseByteData;
import com.decompiler.util.bytestream.ByteData;
import com.decompiler.util.collections.ListFactory;
import com.decompiler.util.collections.MapFactory;
import com.decompiler.util.collections.SetFactory;
import com.decompiler.util.functors.BinaryFunction;
import com.decompiler.util.functors.UnaryFunction;
import com.decompiler.util.getopt.Options;

public class DCCommonState {

    private final ClassCache classCache;
    private final ClassFileSource2 classFileSource;
    private final Options options;
    private final Map<String, ClassFile> classFileCache;
    private Set<JavaTypeInstance> versionCollisions;
    private transient LinkedHashSet<String> couldNotLoadClasses = new LinkedHashSet<String>();
    private final ObfuscationMapping obfuscationMapping;
    private final OverloadMethodSetCache overloadMethodSetCache;

    public DCCommonState(Options options, ClassFileSource2 classFileSource) {
        this.options = options;
        this.classFileSource = classFileSource;
        this.classCache = new ClassCache(this);
        this.classFileCache = MapFactory.newExceptionRetainingLazyMap(new UnaryFunction<String, ClassFile>() {
            @Override
            public ClassFile invoke(String arg) {
                return loadClassFileAtPath(arg);
            }
        });
        this.versionCollisions = SetFactory.newSet();
        this.obfuscationMapping = NullMapping.INSTANCE;
        this.overloadMethodSetCache = new OverloadMethodSetCache();
    }

    public DCCommonState(DCCommonState dcCommonState, final BinaryFunction<String, DCCommonState, ClassFile> cacheAccess) {
        this.options = dcCommonState.options;
        this.classFileSource = dcCommonState.classFileSource;
        this.classCache = new ClassCache(this);
        this.classFileCache = MapFactory.newExceptionRetainingLazyMap(new UnaryFunction<String, ClassFile>() {
            @Override
            public ClassFile invoke(String arg) {
                return cacheAccess.invoke(arg, DCCommonState.this);
            }
        });
        this.versionCollisions = dcCommonState.versionCollisions;
        this.obfuscationMapping = dcCommonState.obfuscationMapping;
        this.overloadMethodSetCache = dcCommonState.overloadMethodSetCache;
    }

    // TODO : If we have any more of these, refactor to a builder!
    public DCCommonState(DCCommonState dcCommonState, ObfuscationMapping mapping) {
        this.options = dcCommonState.options;
        this.classFileSource = dcCommonState.classFileSource;
        this.classCache = new ClassCache(this);
        this.classFileCache = MapFactory.newExceptionRetainingLazyMap(new UnaryFunction<String, ClassFile>() {
            @Override
            public ClassFile invoke(String arg) {
                return loadClassFileAtPath(arg);
            }
        });
        this.versionCollisions = dcCommonState.versionCollisions;
        this.obfuscationMapping = mapping;
        this.overloadMethodSetCache = dcCommonState.overloadMethodSetCache;
    }

    public void setCollisions(Set<JavaTypeInstance> versionCollisions) {
        this.versionCollisions = versionCollisions;
    }

    public Set<JavaTypeInstance> getVersionCollisions() {
        return versionCollisions;
    }

    public void configureWith(ClassFile classFile) {
        classFileSource.informAnalysisRelativePathDetail(classFile.getUsePath(), classFile.getFilePath());
    }

    String getPossiblyRenamedFileFromClassFileSource(String name) {
        return classFileSource.getPossiblyRenamedPath(name);
    }

    @SuppressWarnings("unused")
    public Set<String> getCouldNotLoadClasses() {
        return couldNotLoadClasses;
    }

    public ClassFile loadClassFileAtPath(final String path) {
        try {
            Pair<byte[], String> content = classFileSource.getClassFileContent(path);
            ByteData data = new BaseByteData(content.getFirst());
            return new ClassFile(data, content.getSecond(), this);
        } catch (Exception e) {
            couldNotLoadClasses.add(path);
            throw new CannotLoadClassException(path, e);
        }
    }

    public DecompilerComment renamedTypeComment(String typeName) {
        String originalName = classCache.getOriginalName(typeName);
        if (originalName != null) {
            return new DecompilerComment("Renamed from " + originalName);
        }
        return null;
    }

    private static boolean isMultiReleaseJar(JarContent jarContent) {
        String val = jarContent.getManifestEntries().get(MiscConstants.MULTI_RELEASE_KEY);
        if (val == null) return false;
        return Boolean.parseBoolean(val);
    }

    public TreeMap<Integer, List<JavaTypeInstance>> explicitlyLoadJar(String path, AnalysisType type) {
        JarContent jarContent = classFileSource.addJarContent(path, type);

        TreeMap<Integer, List<JavaTypeInstance>> baseRes = MapFactory.newTreeMap();
        Map<Integer, List<JavaTypeInstance>> res = MapFactory.newLazyMap(baseRes, new UnaryFunction<Integer, List<JavaTypeInstance>>() {
            @Override
            public List<JavaTypeInstance> invoke(Integer arg) {
                return ListFactory.newList();
            }
        });
        boolean isMultiReleaseJar = isMultiReleaseJar(jarContent);

        for (String classPath : jarContent.getClassFiles()) {
            // If the classPath is from a multi release jar, then we're going
            // to have to process it in a more unpleasant way.
            int version = 0;
            if (isMultiReleaseJar) {
                Matcher matcher = MiscConstants.MULTI_RELEASE_PATH_PATTERN.matcher(classPath);
                // It's kind of irritating that we're reprocessing each name, rather than
                // determining this in a tree structured walk through the source jar.
                if (matcher.matches()) {
                    try {
                        String ver = matcher.group(1);
                        version = Integer.parseInt(ver);
                        classPath = matcher.group(2);
                    } catch (Exception e) {
                        // This is unfortunate - someone's playing silly buggers!
                        // Ignore this file - it won't get seen by jre.
                        // (should also be impossible to get here given regex).
                        continue;
                    }
                }
            }

            // Redundant test as we're defending against a bad implementation.
            if (classPath.toLowerCase().endsWith(".class")) {
                res.get(version).add(classCache.getRefClassFor(classPath.substring(0, classPath.length() - 6)));
            }
        }
        return baseRes;
    }

    public ClassFile getClassFile(String path) throws CannotLoadClassException {
        return classFileCache.get(path);
    }

    public JavaRefTypeInstance getClassTypeOrNull(String path) {
        try {
            ClassFile classFile = getClassFile(path);
            return (JavaRefTypeInstance) classFile.getClassType();
        } catch (CannotLoadClassException e) {
            return null;
        }
    }

    public ClassFile getClassFile(JavaTypeInstance classInfo) throws CannotLoadClassException {
        String path = classInfo.getRawName();
        path = ClassNameUtils.convertToPath(path) + ".class";
        return getClassFile(path);
    }

    public ClassFile getClassFileOrNull(JavaTypeInstance classInfo) {
        try {
            return getClassFile(classInfo);
        } catch (CannotLoadClassException ignore) {
            return null;
        }
    }

    public ClassFile getClassFileMaybePath(String pathOrName) throws CannotLoadClassException {
        if (pathOrName.endsWith(".class")) {
            // Fine - we're sure it's a class file.
            return getClassFile(pathOrName);
        }
        // See if this file exists - in which case it's odd.
        File f = new File(pathOrName);
        if (f.exists()) {
            return getClassFile(pathOrName);
        }
        return getClassFile(ClassNameUtils.convertToPath(pathOrName) + ".class");
    }


    public ClassCache getClassCache() {
        return classCache;
    }

    public Options getOptions() {
        return options;
    }

    // No fancy file identification right now, just very very simple.
    public AnalysisType detectClsJar(String path) {
        String lcPath = path.toLowerCase();
        if (lcPath.endsWith(".jar")) return AnalysisType.JAR;
        if (lcPath.endsWith(".war")) return AnalysisType.WAR;
        return AnalysisType.CLASS;
    }

    public ObfuscationMapping getObfuscationMapping() {
        return obfuscationMapping;
    }

    public OverloadMethodSetCache getOverloadMethodSetCache() {
        return overloadMethodSetCache;
    }
}