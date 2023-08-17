package org.vadere.gui.components.control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Stack;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

public class HelpTextView extends JEditorPane {

	private ArrayList<String> filenames;
	private Stack<String> helpHistory;
	public static HelpTextView create(String className){
		HelpTextView view = new HelpTextView();
		view.loadHelpFromClass(className);
		return view;
	}

	public HelpTextView() {
		setContentType("text/html");
		setEditable(false);
		addHyperlinkListener(e -> {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED){
				String link = e.getDescription();
				if (link.startsWith("/helpText/")){
					String clsName = link.split("/")[2].strip();
					for(String f : filenames){
						if (f.endsWith(clsName)){
							link = f;
							break;
						}
					}
					loadHelpText(link);
					return;
				}
				if (link.startsWith("/returnToLastPage/")){
					if(helpHistory.size() > 1){
						helpHistory.pop();
						loadHelpText(helpHistory.peek());
					}
					return;
				}
			}
		});
		filenames  = new ArrayList<>();
		helpHistory = new Stack<>();
		try (
				InputStream in = getClass().getResourceAsStream("/helpText");
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
		System.out.println(helpTextId);
		if(helpHistory.isEmpty()) {
			helpHistory.push(helpTextId);
		}
		else if (!helpTextId.equals(helpHistory.peek())) {
			helpHistory.push(helpTextId);
		}
		String text = null;
		try {
			InputStream url = getClass().getResourceAsStream(helpTextId);
			text = new String(url.readAllBytes());
		} catch (Exception ignored) {
			text = "No Help found.";
		}
		HTMLEditorKit htmlEditorKit = new HTMLEditorKit();
		setEditorKit(htmlEditorKit);
		StyleSheet sheet = htmlEditorKit.getStyleSheet();
		sheet.importStyleSheet(getClass().getResource("/docstyle/style.css"));
		setText(text);
	}

}
