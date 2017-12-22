package jp.ats.dexter.plugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "jp.ats.dexter.plugin";

	private static Activator plugin;

	public Activator() {}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return plugin;
	}

	public static Properties getPersistentProperties(IJavaProject project) {
		Properties properties = new Properties();
		try {
			InputStream input = new BufferedInputStream(new FileInputStream(
				getPropertiesFile(project)));
			try {
				properties.load(input);
			} finally {
				input.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return properties;
	}

	public static void storePersistentProperties(
		IJavaProject project,
		Properties properties) {
		try {
			OutputStream output = new BufferedOutputStream(
				new FileOutputStream(getPropertiesFile(project)));
			try {
				properties.store(output, "");
			} finally {
				output.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static File getPropertiesFile(IJavaProject project)
		throws IOException {
		File file = new File(
			project.getProject().getLocation().toFile(),
			".dexter.plugin");
		if (!file.exists()) {
			file.createNewFile();

			OutputStream output = new BufferedOutputStream(
				new FileOutputStream(file));
			try {
				new Properties().store(output, "");
			} finally {
				output.close();
			}
		}
		return file;
	}
}
