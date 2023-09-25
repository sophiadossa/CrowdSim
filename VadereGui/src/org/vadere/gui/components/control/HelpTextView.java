package org.vadere.gui.components.control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Stack;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.web.WebView;
import javafx.scene.Scene;
import netscape.javascript.JSObject;
import org.vadere.gui.components.utils.Resources;
import org.vadere.util.logging.Logger;


public class HelpTextView extends JFXPanel {
	private static final Logger logger = Logger.getLogger(HelpTextView.class);

	private WebView webView;

	private Stack<String> history;

	private String javaScriptBlock;

	public static HelpTextView create(String className){
		HelpTextView view = new HelpTextView();
		view.loadHelpFromClass(className);
		return view;
	}

	public static boolean exists(String className){
		HelpTextView view = new HelpTextView();
		InputStream instream = view.getClass().getResourceAsStream("/helpText/" + className + ".html");
		return instream != null;
	}

	public HelpTextView() {
		this.history = new Stack<>();
		this.javaScriptBlock = buildJavaScriptBlock();
	}

	// load all js files from the js folder
	// necessary since WebEngine does not support external js file loading
	private String buildJavaScriptBlock() {
		StringBuilder script = new StringBuilder();
		try {
			Files.walkFileTree(Path.of(getClass().getResource("/js").getPath()), new JSConcatter(script));
		} catch (IOException e) {

		}
		return "<script>" + script + "</script>";
	}
	public void loadHelpFromClass(String fullClassName){
		loadHelpText("/helpText/" + fullClassName + ".html");
	}

	public void loadHelpText(String helpTextId){
		if(history.empty()){
			this.history.push(helpTextId);
		}else{
			if(!history.peek().equals(helpTextId)){
				this.history.push(helpTextId);
			}
		}
		InputStream instream = getClass().getResourceAsStream(helpTextId);
		var ref = new Object() {
			String html = null;
		};
		try {
			ref.html = new String(instream.readAllBytes());
			ref.html = ref.html.replace("{{javascript}}", javaScriptBlock);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Platform.runLater(() -> {
			checkWebViewRunning();
			webView.getEngine().loadContent(ref.html);
		});
	}

	private void checkWebViewRunning() {
		if(webView == null) {
			webView = new WebView();
			setScene(new Scene(webView));
			registerLinkEvent();
			loadStyleSheet();
		}
	}

	private void registerLinkEvent() {
		webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
			if (newState == Worker.State.SUCCEEDED) {
				JSObject jsobj = (JSObject) webView.getEngine().executeScript("window");
				jsobj.setMember("java", this);
				webView.getEngine().executeScript("" +
						"document.addEventListener('click', function(e) {" +
							"let anchor = e.composedPath().find(el => el.tagName === 'A');" +
							"if(anchor) {" +
								"e.preventDefault();" +
								"java.handleLinkClick(anchor.href);" +
							"}" +
						"});"
				);

			}
		});
	}
	@SuppressWarnings("unused")
	public void handleLinkClick(String href) {
		if (href.startsWith("/helpText/")) {
			loadHelpText(href);
			//this.history.push(href);
		} else if (href.startsWith("/back")) {
			if(this.history.size() > 1) {
				this.history.pop();
				loadHelpText(this.history.peek());
			}
		}
	}

	public void loadStyleSheet(){
		Platform.runLater(() -> {
			webView.getEngine().setUserStyleSheetLocation(getClass().getResource("/docstyle/style.css").toString());
		});
	}

	private static class JSConcatter implements FileVisitor<Path> {
		private final StringBuilder script;

		public JSConcatter(StringBuilder script) {
			this.script = script;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			if (file.getFileName().toString().endsWith(".js")){
				script.append(new String(Files.readAllBytes(file)));
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}
	}
}
