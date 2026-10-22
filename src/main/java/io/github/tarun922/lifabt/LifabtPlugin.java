package dev.tarun.lifabt;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

public class LifabtPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        LifabtExtension ext = project.getExtensions().create("lifabt", LifabtExtension.class);
        ext.getVersion().convention("latest");
        ext.getPrefix().convention("lucide_");

        TaskProvider<GenerateLucideIconsTask> gen = project.getTasks().register(
                "generateLucideIcons", GenerateLucideIconsTask.class, t -> {
                    t.setGroup("lifabt");
                    t.setDescription("Downloads (first run) and converts Lucide icons to VectorDrawables.");
                    t.getIcons().set(ext.getIcons());
                    t.getVersion().set(ext.getVersion());
                    t.getPrefix().set(ext.getPrefix());
                    t.getOutputDir().set(project.getLayout().getBuildDirectory().dir("generated/lucide/res"));
                    t.getCacheDir().set(project.getLayout().getProjectDirectory().dir("lucide-icons"));
                });

        project.getTasks().matching(t -> t.getName().equals("preBuild"))
                .configureEach(t -> t.dependsOn(gen));

        Runnable wire = () -> {
            Object android = project.getExtensions().findByName("android");
            if (android == null) {
                throw new org.gradle.api.GradleException(
                        "dev.tarun.lifabt must be applied together with com.android.application or com.android.library");
            }
            try {
                Object sourceSets = InvokerHelper.getProperty(android, "sourceSets");
                Object main = InvokerHelper.invokeMethod(sourceSets, "getByName", "main");
                Object res = InvokerHelper.getProperty(main, "res");
                InvokerHelper.invokeMethod(res, "srcDir", gen.flatMap(GenerateLucideIconsTask::getOutputDir));
            } catch (RuntimeException e) {
                project.getLogger().warn("lifabt: could not auto-register the res folder (" + e.getMessage()
                        + "). Add this to your android block: sourceSets.main.res.srcDir(\"build/generated/lucide/res\")");
            }
        };
        project.getPlugins().withId("com.android.application", p -> project.afterEvaluate(x -> wire.run()));
        project.getPlugins().withId("com.android.library", p -> project.afterEvaluate(x -> wire.run()));
    }
}
