package org.vadere.gui.components.control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Stack;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.web.WebView;
import javafx.scene.Scene;
import netscape.javascript.JSObject;


public class HelpTextView extends JFXPanel {

	private ArrayList<String> filenames;

	private WebView webView;

	private Stack<String> history;

	public static HelpTextView create(String className){
		HelpTextView view = new HelpTextView();
		view.loadHelpFromClass(className);
		return view;
	}

	public HelpTextView() {
		this.filenames  = new ArrayList<>();
		this.history = new Stack<>();
		extractFiles();
	}

	private void extractFiles() {
		try (InputStream in = getClass().getResourceAsStream("/helpText");
			 BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
			String resource;
			while ((resource = br.readLine()) != null) {
				filenames.add(resource);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
				webView.getEngine().executeScript(
						"document.addEventListener('click', function(e) { " +
								"if(e.target.tagName === 'A') { " +
									"if(e.target.href.startsWith('/helpText/')){" +
										"e.preventDefault();" +
									"} " +
									"java.handleLinkClick(e.target.href); " +
								"} " +
						"});");

			}
		});
	}
	@SuppressWarnings("unused")
	public void handleLinkClick(String href) {
		System.out.println("Link clicked: " + href);
		if (href.startsWith("/helpText/")) {
			loadHelpText(href);
			//this.history.push(href);
		} else if (href.startsWith("/back")) {
			if(this.history.size() > 1) {
				this.history.pop();
				loadHelpText(this.history.peek());
				System.out.println(this.history);
			}
		}
	}

	public void loadStyleSheet(){
		Platform.runLater(() -> {
			webView.getEngine().setUserStyleSheetLocation(getClass().getResource("/docstyle/style.css").toString());
		});
	}

}
