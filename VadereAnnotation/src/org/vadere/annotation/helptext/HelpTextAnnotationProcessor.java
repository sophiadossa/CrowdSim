package org.vadere.annotation.helptext;

import com.google.auto.service.AutoService;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.ImportScanner;
import org.vadere.util.reflection.VadereAttribute;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import static org.vadere.util.other.Strings.removeAttribute;
import static org.vadere.util.other.Strings.splitCamelCase;

@SupportedAnnotationTypes({"*"}) // run for all annotations. process must return false so annotations are not consumed
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class HelpTextAnnotationProcessor extends AbstractProcessor {
	ArrayList<Function<String, String>> pattern;
	Set<String> importedTypes;


	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		initPattern();
		ImportScanner scanner = new ImportScanner();
		scanner.scan(roundEnv.getRootElements(), null);
		importedTypes = scanner.getImportedTypes();
		for (Element e: roundEnv.getRootElements()){
			if ((e.getKind().isClass())  && e.asType().toString().startsWith("org.vadere.")) {
				for(Element f : e.getEnclosedElements()){
					if(f.getKind().isField()){
						try {
							//String comment = processingEnv.getElementUtils().getDocComment(e);
							//String relname = buildHelpTextPath(e.asType().toString());
							String comment = processingEnv.getElementUtils().getDocComment(f);
							String relname = buildFieldHelpTextPath(e,f);
							FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", relname);
							try (PrintWriter w = new PrintWriter(file.openWriter())) {
								printSingleMemberString(f,w);
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}

					}
				}
				try {
					String comment = processingEnv.getElementUtils().getDocComment(e);
					String relname = buildClassHelpTextPath(e);
					FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", relname);
					try (PrintWriter w = new PrintWriter(file.openWriter())) {
						composeHTMLBegin(w);
						composeHTMLHeader(e, w);
						composeHTMLClassDscription(w, comment);
						composeHTMLEnd(e, w);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}

			}

		}
		return false; // allow further processing
	}

	private void composeHTMLEnd(Element e, PrintWriter w) {
		printMemberDocString(e, w);
		w.println("</div>"); // main
		w.println("</body>");
		w.println("</html>");
	}

	private void composeHTMLClassDscription(PrintWriter w, String comment) {
		w.println("<div class='comment'>");
		printComment(w, comment);
		w.println("</div>"); // comment
		w.println();
	}

	private void composeHTMLHeader(Element e, PrintWriter w) {
		w.println("<div class='header'>");
		w.println("<a href='/back'>&lt; Back</a>");

		Element superElement = getSuperElement(e);
		if(isIgnorableSuperClass(superElement))
			w.print("<h1> " + composeHeaderName(e)+"</h1>");
		else
			w.println("<h1> " + composeHeaderName(e) +" : "+ composeSuperClassLink(superElement) +"</h1>");
		w.println("</div>"); // header
		w.println();
		w.println("<div class='main'>");
	}

	@NotNull
	private static String composeHeaderName(Element e) {
		return removeAttribute(String.format("%s", e.getSimpleName()));
	}

	private String composeSuperClassLink(Element superElement) {
		return String.format("<a href='%s' class='class_link'>%s</a>", findFullPath(String.format("%s", superElement.getSimpleName())), removeAttribute(String.format("%s", superElement.getSimpleName())));
	}

	private static Element getSuperElement(Element e) {
		return ((DeclaredType)((TypeElement)e).getSuperclass()).asElement();
	}

	private static void composeHTMLBegin(PrintWriter w) {
		w.println("<!DOCTYPE html>");
		w.println("<html>");
		w.println("<head>");
		w.println("<meta charset=\"UTF-16\">");
		w.println("</head>");
		w.println("<body>");
	}

	private String buildClassHelpTextPath(Element e) {
		String className = e.asType().toString();
		className = className.replace("<", "_");
		className = className.replace(">", "_");
		return "helpText/" + className + ".html";
	}
	private String buildFieldHelpTextPath(Element e, Element f) {
		String className = e.asType().toString();
		className = className.replace("<", "_");
		className = className.replace(">", "_");
		String fieldName = f.getSimpleName().toString();
		if (f.getKind() == ElementKind.ENUM_CONSTANT) {
			fieldName = fieldName + "_ENUM";
		}
		return "helpText/" + className +"_"+ fieldName + ".html";
	}
	private void initPattern() {
		pattern = new ArrayList<>();
		pattern.add( e -> {
			Pattern r = Pattern.compile("(\\{@link\\s+#)(.*?)(})");
			Matcher m = r.matcher(e);
			while (m.find()){
				e = m.replaceFirst("<span class='local_link'>$2</span>");
				m = r.matcher(e);
			}
			return e;
		});
		pattern.add( e -> {
			Pattern r = Pattern.compile("(\\{@link\\s+)(.*?)(})");
			Matcher m = r.matcher(e);
			while (m.find()){
				String linkId = findFullPath(m.group(2));
				String fieldType = removeAttribute(stripToBaseString(m.group(2)));
				e = m.replaceFirst(String.format("<a href='%s' class='class_link'>%s</a>", linkId,fieldType));
				m = r.matcher(e);
			}
			return e;
		});

	}

	private String findFullPath(String className){
		String n = importedTypes.stream().filter(e-> e.endsWith(className)).findFirst().orElse(className);
		return "/helpText/" + n + ".html";
	}

	private void printComment(PrintWriter w, String multiLine){
		if(multiLine != null){
			w.println("<p>");
			multiLine.lines().map(String::strip).map(this::applyMatcher).forEach(w::println);
			w.println("</p>");
		}
	}

	private String applyMatcher(String line){
		for(Function<String, String> p : pattern){
			line = p.apply(line);
		}
		return line;
	}

	private void printMemberDocString(Element e, PrintWriter w) {
		Set<? extends Element> fields = e.getEnclosedElements()
				.stream()
				.filter(o->o.getKind().isField())
				.collect(Collectors.toSet());
		for(Element field : fields){
			if(field.getAnnotation(VadereAttribute.class) != null && field.getAnnotation(VadereAttribute.class).exclude())
				continue;
			w.println("<div class='param'>");
			String typeString;
			if(isPrimitiveType(field)) {
				typeString = getTypeString(field);
				typeString = typeString.replace("java.util.","");
				typeString = typeString.replace("java.lang.","");
			}else{
				typeString = String.format("<a href='%s' class='class_link'>%s</a>",findFullPath(getTypeString(field)),strippedTypeString(field));
			}
			w.println("<h2>" + field.getSimpleName() + " : " + typeString +  "</h2>");
			String comment = processingEnv.getElementUtils().getDocComment(field);
			printComment(w, comment);
			w.println("</div>");
			w.println();
		}

	}

	private void printSingleMemberString(Element e, PrintWriter w) {
			String comment = processingEnv.getElementUtils().getDocComment(e);
			w.println("<b>" + e.getSimpleName() + ":</b><br>" + comment);
	}

	private boolean isPrimitiveType(Element field){
		return field.asType().getKind().isPrimitive() || !field.asType().toString().startsWith("org.vadere");
	}

	private String getTypeString(Element field){
		return field.asType().toString();
	}

	private String strippedTypeString(Element field){
		var str = field.asType().toString();
		str = str.substring(str.lastIndexOf(".") + 1);
		if(str.startsWith("Attribute")){
			str = str.substring("Attributes".length());
		}
		return str;
	}

	private String stripToBaseString(String str){
		return str.substring(str.lastIndexOf(".") + 1);
	}

	private boolean isIgnorableSuperClass(Element e){
		checkElementIsClass(e);
		return Arrays.stream(new String[]{
				"Object",
				"Enum",
				"Cloneable",
				"Serializable",
				"Attributes"
		}).anyMatch(s -> s.equals(e.getSimpleName().toString()));
	}

	private static void checkElementIsClass(Element e) {
		if(e.getKind() != ElementKind.CLASS)
			throw new IllegalArgumentException("Unexpected usage of this function, parameter must be an element of kind 'Class'.");
	}
}
