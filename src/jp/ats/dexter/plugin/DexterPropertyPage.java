package jp.ats.dexter.plugin;

import static jp.ats.dexter.plugin.Constants.JSPOD_DEFAULT_ID;
import static jp.ats.dexter.plugin.Constants.JSPOD_PARENT;
import static jp.ats.dexter.plugin.Constants.JSPOD_PREFIXER;
import static jp.ats.substrate.U.isAvailable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbenchPropertyPage;

public class DexterPropertyPage extends FieldEditorPreferencePage
	implements IWorkbenchPropertyPage {

	private IJavaProject element;

	public DexterPropertyPage() {
		super(GRID);
		setDescription(Constants.TITLE + " 設定");
	}

	@Override
	public boolean performOk() {
		boolean ok = super.performOk();
		IPreferenceStore store = getPreferenceStore();
		Properties properties = Activator.getPersistentProperties(element);

		boolean changed = false;

		changed |= checkAndSetValue(store, properties, JSPOD_PARENT);

		changed |= checkAndSetValue(store, properties, JSPOD_PREFIXER);

		changed |= checkAndSetValue(store, properties, JSPOD_DEFAULT_ID);

		initializeDefaults();

		if (changed) Activator.storePersistentProperties(element, properties);

		return ok;
	}

	@Override
	public IAdaptable getElement() {
		return element;
	}

	@Override
	public void setElement(IAdaptable element) {
		if (element instanceof IProject) {
			try {
				this.element = (IJavaProject) ((IProject) element).getNature(JavaCore.NATURE_ID);
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}
		} else {
			this.element = (IJavaProject) element;
		}
		setPreferenceStore(createPreferenceStore());
		initializeDefaults();
	}

	@Override
	protected void createFieldEditors() {
		StringFieldEditor srcRootEditor = new ClassFieldEditor(
			JSPOD_PARENT,
			"Jspod生成クラスの親クラス (&P)",
			element.getProject(),
			getFieldEditorParent());
		srcRootEditor.setEmptyStringAllowed(false);
		addField(srcRootEditor);

		StringFieldEditor prefixerEditor = new StringFieldEditor(
			JSPOD_PREFIXER,
			"JSP内のJspod識別子 (&P)",
			getFieldEditorParent());
		prefixerEditor.setEmptyStringAllowed(false);
		addField(prefixerEditor);

		StringFieldEditor defaultIdEditor = new StringFieldEditor(
			JSPOD_DEFAULT_ID,
			"JSP内のデフォルトJspod変数名 (&I)",
			getFieldEditorParent());
		defaultIdEditor.setEmptyStringAllowed(false);
		addField(defaultIdEditor);
	}

	private static boolean checkAndSetValue(
		IPreferenceStore store,
		Properties properties,
		String key) {
		String oldValue = properties.getProperty(key);
		String newValue = store.getString(key);
		if (newValue.equals(oldValue)) return false;
		properties.setProperty(key, newValue);
		return true;
	}

	private void initializeDefaults() {
		IPreferenceStore store = getPreferenceStore();

		String parent = store.getString(JSPOD_PARENT);
		if (!isAvailable(parent)) {
			parent = Constants.JSPOD_DEFAULT_PARENT_VALUE;
		}
		store.setDefault(JSPOD_PARENT, parent);

		String prefixer = store.getString(JSPOD_PREFIXER);
		if (!isAvailable(prefixer)) {
			prefixer = Constants.JSPOD_DEFAULT_PREFIXER_VALUE;
		}
		store.setDefault(JSPOD_PREFIXER, prefixer);

		String defaultId = store.getString(JSPOD_DEFAULT_ID);
		if (!isAvailable(defaultId)) {
			defaultId = Constants.JSPOD_DEFAULT_ID_VALUE;
		}
		store.setDefault(JSPOD_DEFAULT_ID, defaultId);
	}

	private PreferenceStore createPreferenceStore() {
		Properties properties = Activator.getPersistentProperties(element);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PreferenceStore store = new PreferenceStore() {

			@Override
			public void save() {}
		};
		try {
			properties.store(out, null);
			store.load(new ByteArrayInputStream(out.toByteArray()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return store;
	}
}
