package jp.ats.dexter.plugin;

import static jp.ats.dexter.plugin.Common.select;
import static jp.ats.substrate.U.isAvailable;
import static jp.ats.substrate.util.ClassBuilderUtilities.convertToTemplate;
import static jp.ats.substrate.util.ClassBuilderUtilities.pickupFromSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import jp.ats.dexter.DexterManager;
import jp.ats.dexter.plugin.Jspod.Property;
import jp.ats.substrate.U;
import jp.ats.substrate.util.CollectionMap;

public class JspodAction implements IObjectActionDelegate {

	private static final Pattern importPattern = Pattern
		.compile("<%@\\s*page\\s+import=\"([^\"]+)\"\\s*%>");

	private IResource resource;

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {}

	@Override
	public void run(IAction action) {
		try {
			execute(resource);
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection == null) return;
		IStructuredSelection structured = (IStructuredSelection) selection;
		resource = (IResource) structured.getFirstElement();
	}

	private static void execute(IResource resource) throws CoreException {
		if (resource instanceof IFile) {
			IFile file = (IFile) resource;
			String extention = file.getFileExtension();
			if ("jsp".equalsIgnoreCase(extention)
				|| "jspf".equalsIgnoreCase(extention)) {
				execute(file);
			}
		} else if (resource instanceof IContainer) {
			for (IResource myResource : ((IContainer) resource).members()) {
				execute(myResource);
			}
		}
	}

	private static void execute(IFile jsp) {
		try {
			InputStream source = jsp.getContents(false);
			String charset = jsp.getCharset();

			IJavaProject project;
			try {
				project = (IJavaProject) jsp.getProject()
					.getNature(JavaCore.NATURE_ID);
			} catch (CoreException e) {
				throw new RuntimeException(e);
			}

			String original = new String(U.readBytes(source), charset);
			String content = processAll(original, project);

			if (content.equals(original)) return;

			jsp.setContents(
				new ByteArrayInputStream(content.getBytes(charset)),
				false,
				true,
				null);
		} catch (Exception e) {
			e.printStackTrace();
			MessageDialog.openError(null, Constants.TITLE, e.getMessage());
			return;
		}
	}

	private static String processAll(String content, IJavaProject project)
		throws JavaModelException {
		Properties properties = Activator.getPersistentProperties(project);

		String prefixer = properties.getProperty(
			Constants.JSPOD_PREFIXER,
			Constants.JSPOD_DEFAULT_PREFIXER_VALUE);

		String defaultId = properties.getProperty(
			Constants.JSPOD_DEFAULT_ID,
			Constants.JSPOD_DEFAULT_ID_VALUE);

		String parent = properties.getProperty(
			Constants.JSPOD_PARENT,
			Constants.JSPOD_DEFAULT_PARENT_VALUE);

		Map<String, Jspod> jspods = new HashMap<>();
		content = processJspod(jspods, prefixer, defaultId, content, project);
		for (Jspod jspod : jspods.values()) {
			content = jspod.processProperty(prefixer, content);
			createClass(jspod, parent, project);

			content = jspod.processFormId(prefixer, content);
			content = jspod.processTokenTag(prefixer, content);
		}

		return content;
	}

	private static void createClass(
		Jspod jspod,
		String parent,
		IJavaProject project) throws JavaModelException {
		String packageName = getPackageName(jspod.className);

		IPackageFragment fragment;
		IJavaElement element = project
			.findElement(new Path(packageName.replace('.', '/')));
		if (!(element instanceof IPackageFragment)) {
			IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
			List<IPackageFragmentRoot> srcRoots = new LinkedList<IPackageFragmentRoot>();
			for (IPackageFragmentRoot root : roots) {
				if (root.getKind() != IPackageFragmentRoot.K_SOURCE) continue;
				srcRoots.add(root);
			}

			if (srcRoots.size() != 1) throw new IllegalStateException(
				"パッケージ " + packageName + " を作成するためのパッケージルートが複数あります");

			fragment = srcRoots.get(0)
				.createPackageFragment(packageName, false, null);
		} else {
			fragment = (IPackageFragment) element;
		}

		String className = getClassName(jspod.className);

		String source = readTemplate();

		String templateImports;
		{
			String[] result = pickupFromSource(source, "JspodImportsPart");
			templateImports = result[0];
			source = result[1];
		}

		String propertiesPart;
		{
			String[] result = pickupFromSource(source, "JspodPropertiesPart");
			propertiesPart = convertToTemplate(result[0]);
			source = result[1];
		}

		String mapPropertiesPart;
		{
			String[] result = pickupFromSource(
				source,
				"JspodMapPropertiesPart");
			mapPropertiesPart = convertToTemplate(result[0]);
			source = result[1];
		}

		String applyPart;
		{
			String[] result = pickupFromSource(source, "JspodApplyPart");
			applyPart = convertToTemplate(result[0]);
			source = result[1];
		}

		String setterGetterPart;
		{
			String[] result = pickupFromSource(source, "JspodSetterGetterPart");
			setterGetterPart = convertToTemplate(result[0]);
			source = result[1];
		}

		source = convertToTemplate(source);

		String compilationUnitName = className + ".java";
		ICompilationUnit compilationUnit = fragment
			.getCompilationUnit(compilationUnitName);

		Set<String> imports = new HashSet<>();
		List<String> typeAnnotations = new LinkedList<>();
		CollectionMap<String, String> annotations = new CollectionMap<>();

		if (compilationUnit.exists()) {
			IType type = compilationUnit.getType(className);
			for (IAnnotation annotation : type.getAnnotations()) {
				typeAnnotations.add(annotation.getSource());
			}

			for (IImportDeclaration importDeclaration : compilationUnit
				.getImports()) {
				imports.add(importDeclaration.getSource());
			}

			for (IField field : compilationUnit.getAllTypes()[0].getFields()) {
				for (IAnnotation annotation : field.getAnnotations()) {
					annotations
						.put(field.getElementName(), annotation.getSource());
				}
			}
		}

		List<String> propertiesPartList = new LinkedList<>();
		List<String> mapPropertiesPartList = new LinkedList<>();
		List<String> applyPartList = new LinkedList<>();
		List<String> setterGetterPartList = new LinkedList<>();
		for (Property property : jspod.getProperties()) {
			if (property.type.needsImport())
				imports.add(property.type.getImportString());

			String typeString = property.type.clazz.getSimpleName();

			Collection<String> myAnnotations = annotations.get(property.name);
			propertiesPartList.add(
				MessageFormat.format(
					propertiesPart,
					String.join(U.LINE_SEPARATOR, myAnnotations),
					property.name,
					typeString));

			String nameMap = property.name + "Map";

			mapPropertiesPartList.add(
				MessageFormat.format(
					mapPropertiesPart,
					nameMap,
					property.type.getWrapperName()));

			applyPartList.add(
				MessageFormat.format(
					applyPart,
					nameMap,
					"\"" + property.name + "\"",
					property.name,
					property.type.getConverterName()));

			setterGetterPartList.add(
				MessageFormat.format(
					setterGetterPart,
					Jspod.capitalize(property.name),
					property.name,
					property.name + "Map",
					typeString));
		}

		String completed = MessageFormat.format(
			source,
			packageName,
			imports.size() > 0
				? String.join(U.LINE_SEPARATOR, imports)
				: templateImports,
			String.join(U.LINE_SEPARATOR, typeAnnotations),
			className,
			parent,
			String.join("", propertiesPartList),
			String.join("", mapPropertiesPartList),
			String.join("", applyPartList),
			String.join("", setterGetterPartList));

		Document document = new Document(completed);
		try {
			ToolFactory.createCodeFormatter(project.getOptions(true))
				.format(
					CodeFormatter.K_COMPILATION_UNIT,
					completed,
					0,
					completed.length(),
					0,
					null)
				.apply(document);
		} catch (BadLocationException e) {
			throw new IllegalStateException(e);
		}

		completed = document.get();

		if (compilationUnit.exists()
			&& completed.equals(compilationUnit.getSource())) return;

		fragment
			.createCompilationUnit(compilationUnitName, completed, true, null);
	}

	public static String readTemplate() {
		URL templateURL = U.getResourcePathByName(
			Template.class,
			Template.class.getSimpleName() + ".java");
		try {
			return new String(U.readBytes(templateURL.openStream()));
		} catch (IOException e) {
			throw new Error();
		}
	}

	private static String processJspod(
		Map<String, Jspod> jspods,
		String prefixer,
		String defaultId,
		String content,
		IJavaProject project) throws JavaModelException {
		String smallPattern = prefixer + "\\{(\\w*:|)([^\\}]+)\\}";

		Pattern pattern = Pattern.compile(
			"<% */\\* ("
				+ smallPattern
				+ ") \\*/[^%]+%>|("
				+ smallPattern
				+ ")");

		StringBuilder converted = new StringBuilder();
		int previous = 0;

		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
			converted.append(content.substring(previous, matcher.start()));
			previous = matcher.end();

			String id = select(matcher.group(2), matcher.group(5));

			boolean isIdAvailable;
			if (isAvailable(id)) {
				//末尾の:を除去
				id = id.substring(0, id.length() - 1);
				isIdAvailable = true;
			} else {
				id = defaultId;
				isIdAvailable = false;
			}

			String className = select(matcher.group(3), matcher.group(6));

			IType type = project.findType(className);
			List<String> properties = new LinkedList<>();
			if (type != null) {
				IField[] fields = type.getFields();
				for (IField field : fields) {
					int flags = field.getFlags();
					if (Flags.isPublic(flags)
						&& Flags.isStatic(flags)
						&& Flags.isFinal(flags))
						properties.add(field.getElementName());
				}
			}

			Jspod jspod = new Jspod(
				id,
				className,
				properties.toArray(new String[properties.size()]),
				isIdAvailable);

			String template = "<% /* {0} */ {1} {2} = ({1}) "
				+ removePackage(content, DexterManager.class.getName())
				+ ".newJspod({1}.class); %>";
			String processed = MessageFormat.format(
				template,
				select(matcher.group(1), matcher.group(4)),
				removePackage(content, jspod.className),
				id);

			converted.append(processed);

			jspods.put(id, jspod);
		}

		converted.append(content.substring(previous));

		return converted.toString();
	}

	private static String getPackageName(String className) {
		return className.replaceFirst("\\.[^\\.]+$", "");
	}

	private static String getClassName(String className) {
		return className.replaceAll("^.+?([^\\.]+)$", "$1");
	}

	private static String removePackage(String content, String className) {
		Matcher matcher = importPattern.matcher(content);
		while (matcher.find()) {
			for (String name : matcher.group(1).split(" *; *")) {
				name = name.trim().replaceFirst("\\.\\*$", "");
				if (name.equals(className)
					|| name.equals(getPackageName(className)))
					return getClassName(className);
			}
		}

		return className;
	}
}
