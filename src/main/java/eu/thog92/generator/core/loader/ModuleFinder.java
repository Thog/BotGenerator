package eu.thog92.generator.core.loader;

import eu.thog92.generator.api.annotations.Module;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class ModuleFinder
{

    private final List<String> blackListedPackage = new ArrayList<>();

    private final HashMap<String, Module> annotationCache = new HashMap<>();

    public ModuleFinder()
    {
        blackListedPackage.add("com/intellij");
        blackListedPackage.add("com/oracle");
        blackListedPackage.add("com/sun");
        blackListedPackage.add("com/google");
        blackListedPackage.add("java/");
        blackListedPackage.add("javafx/");
        blackListedPackage.add("javax/");
        blackListedPackage.add("jdk/");
        blackListedPackage.add("netscape");
        blackListedPackage.add("oracle");
        blackListedPackage.add("org/jcp");
        blackListedPackage.add("org/omg");
        blackListedPackage.add("org/xml");
        blackListedPackage.add("org/w3c");
        blackListedPackage.add("sun/");
        blackListedPackage.add("twitter4j/");
        blackListedPackage.add("javassist/");
        blackListedPackage.add("io/netty");
    }

    private Map<String, Class> findClasses(File directory, String packageName)
    {
        if (packageName.startsWith("."))
            packageName = packageName.substring(1);

        Map<String, Class> classes = new HashMap<>();

        if (!directory.exists())
        {
            return classes;
        }

        for (File file : directory.listFiles())
        {
            if (file.isDirectory())
            {
                classes.putAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class"))
            {
                try
                {
                    String name = file.getName().substring(0, file.getName().length() - 6);
                    Class clazz = Class.forName(packageName + '.' + name);
                    if (this.containAnnotInClass(clazz))
                    {
                        Module module = (Module) clazz.getAnnotation(Module.class);
                        classes.put(module.name(), clazz);
                        this.annotationCache.put(module.name(), module);
                    }

                } catch (ClassNotFoundException ignored)
                {
                }
            }
        }
        return classes;
    }

    public Map<String, Class> search()
    {
        return this.start();
    }

    public Module getAnnotFromClass(String name)
    {
        return this.annotationCache.get(name);
    }

    private Map<String, Class> start()
    {
        // Add modules dir to classpath
        URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> classLoaderClass = URLClassLoader.class;
        File moduleDir = new File("modules");
        if (!moduleDir.exists())
            moduleDir.mkdirs();
        for (File file : moduleDir.listFiles())
        {
            if (file.getName().endsWith(".jar"))
            {
                try
                {
                    Method method = classLoaderClass.getDeclaredMethod("addURL", URL.class);
                    method.setAccessible(true);
                    method.invoke(systemClassLoader, file.toURI().toURL());
                } catch (Throwable t)
                {
                    t.printStackTrace();
                }
            }
        }
        Map<String, Class> classes = new HashMap<>();

        try
        {
            for (URL url : systemClassLoader.getURLs())
            {
                String classpathEntry = url.getPath().replace("%20", " ");
                System.out.println("Scanning " + classpathEntry);
                File entryFile = new File(classpathEntry);
                if (classpathEntry.endsWith(".jar"))
                {
                    File jar = new File(classpathEntry);

                    // Don't scan internal libs
                    if (jar.getPath().contains("jre" + File.separator + "lib"))
                        continue;

                    JarInputStream is = new JarInputStream(new FileInputStream(jar));

                    JarEntry entry;
                    while ((entry = is.getNextJarEntry()) != null)
                    {
                        if (entry.getName().endsWith(".class") && !this.isNotBlackListed(entry.getName()))
                        {
                            try
                            {
                                Class clazz = Class.forName(entry.getName().substring(0, entry.getName().length() - 6).replace("/", "."));
                                if (this.containAnnotInClass(clazz))
                                {
                                    Module module = (Module) clazz.getAnnotation(Module.class);
                                    classes.put(module.name(), clazz);

                                    this.annotationCache.put(module.name(), module);
                                }


                            } catch (Exception | NoClassDefFoundError ignored)
                            {
                            }
                        }
                    }
                    is.close();
                }

                // Support IDE and Gradle class dirs
                else if (entryFile.exists() && entryFile.isDirectory())
                {
                    classes.putAll(findClasses(entryFile, ""));
                }
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return classes;
    }

    private boolean isNotBlackListed(String name)
    {
        for (String entry : blackListedPackage)
        {
            if (name.startsWith(entry))
                return true;
        }
        return false;
    }

    private boolean containAnnotInClass(Class clazz)
    {
        for (Annotation annot : clazz.getDeclaredAnnotations())
        {
            if (annot.annotationType() == Module.class)
            {
                return true;
            }
        }
        return false;
    }
}
