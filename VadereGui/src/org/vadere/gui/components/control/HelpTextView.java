package org.vadere.gui.components.control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Stack;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.web.WebView;
import javafx.scene.Scene;
import netscape.javascript.JSObject;


public class HelpTextView extends JFXPanel {

	private WebView webView;

	private Stack<String> history;

	private String javaScriptBlock;

	public static HelpTextView create(String className){
		HelpTextView view = new HelpTextView();
		view.loadHelpFromClass(className);
		return view;
	}

	public HelpTextView() {
		this.history = new Stack<>();
		this.javaScriptBlock = buildJavaScriptBlock();
	}

	// load all js files from the js folder
	// necessary since WebEngine does not support external js file loading
	private String buildJavaScriptBlock() {
		StringBuilder result = new StringBuilder();
		try (InputStream in = getClass().getResourceAsStream("/js");
			 BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
			String resource;
			while ((resource = br.readLine()) != null) {
				InputStream in2 = getClass().getResourceAsStream("/js/"+resource);
				result.append(new String(in2.readAllBytes()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "<script>" + result + "</script>";
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

}
