import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Main {

	public static String getHTML(String urlToRead) throws Exception {
		StringBuilder result = new StringBuilder();
		URL url = new URL(urlToRead);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		rd.close();
		return result.toString();
	}

	public static void putHTML(String urlToPut, String contentToPut) throws Exception {
		URL url = new URL(urlToPut);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("PUT");
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream());
		osw.write(contentToPut);
		osw.flush();
		osw.close();
		System.out.println(connection.getResponseCode());
	}

	static String auroraUrl = "http://<aurora ip>:16021/api/beta/<aurora key>";
	static String effects[];
	static boolean onState;
	static int brightness;
	static int oldBrightness;
	static String auroraCurEffect;

	public static void UpdateAuroraInfo() {
		try {

			String newEffects[] = getHTML(auroraUrl + "/effects/list").replace("[", "").replace("]", "").split(",");
			if (effects != newEffects)
				effects = newEffects;

			String tempState = getHTML(auroraUrl + "/state/on");
			boolean tempOnState = Boolean
					.parseBoolean(tempState.substring(tempState.lastIndexOf(":") + 1, tempState.length() - 1));
			if (onState != tempOnState)
				onState = tempOnState;

			String tempBrightness = getHTML(auroraUrl + "/state/brightness");
			int tempBrightnessVal = Integer
					.parseInt(tempBrightness.substring(tempBrightness.indexOf(":") + 1, tempBrightness.indexOf(",")));
			if (brightness != tempBrightnessVal)
				brightness = tempBrightnessVal;
			
			String tempCurEffect = getHTML(auroraUrl + "/effects/select").replaceAll("\"", "");
			if(auroraCurEffect != tempCurEffect)
				auroraCurEffect = tempCurEffect;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void AuroraInfo() {
		try {
			effects = getHTML(auroraUrl + "/effects/list").replace("[", "").replace("]", "").split(",");

			String tempState = getHTML(auroraUrl + "/state/on");
			onState = Boolean.parseBoolean(tempState.substring(tempState.lastIndexOf(":") + 1, tempState.length() - 1));

			String tempBrightness = getHTML(auroraUrl + "/state/brightness");
			brightness = Integer
					.parseInt(tempBrightness.substring(tempBrightness.indexOf(":") + 1, tempBrightness.indexOf(",")));

			String tempCurEffect = getHTML(auroraUrl + "/effects/select").replaceAll("\"", "");
			if(auroraCurEffect != tempCurEffect)
				auroraCurEffect = tempCurEffect;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static JFrame controlFrame;
	static JPanel controlPanel = new JPanel();
	static JButton onStateButton;
	static JSlider brightnessSlider;

	public static void JFrameUpdate() {
		if(onState)
			onStateButton.setText("ON");
		else
			onStateButton.setText("OFF");
	}

	public static void JFrameSetup() {
		controlFrame = new JFrame("Nanoleaf Aurora Control Panel");
		controlFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		controlFrame.setPreferredSize(new Dimension(975, 250));

		JLabel onOffLabel = new JLabel("On State: ");
		onOffLabel.setBounds(20, 20, 100, 30);
		controlPanel.add(onOffLabel);
		
		onStateButton = new JButton("" + onState);
		onStateButton.setBounds(20, 20, 100, 30);
		onStateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					putHTML(auroraUrl + "/state", "{\"on\":" + !onState + "}");
					String tempState = getHTML(auroraUrl + "/state/on");
					onState = Boolean
							.parseBoolean(tempState.substring(tempState.lastIndexOf(":") + 1, tempState.length() - 1));

				} catch (Exception e1) {
					e1.printStackTrace();
				}
				onStateButton.setText("" + onState);
			}
		});
		controlPanel.add(onStateButton);

		final int FPS_MIN = 0;
		final int FPS_MAX = 100;
		final int FPS_INIT = brightness;

		JLabel brightnessLabel = new JLabel("Brightness: ");
		brightnessLabel.setBounds(20, 20, 100, 30);
		controlPanel.add(brightnessLabel);
		
		brightnessSlider = new JSlider(JSlider.VERTICAL, FPS_MIN, FPS_MAX, FPS_INIT);

		// Turn on labels at major tick marks.
		brightnessSlider.setMajorTickSpacing(10);
		brightnessSlider.setPaintLabels(true);
		brightnessSlider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					int fps = (int) source.getValue();
					try {
						putHTML(auroraUrl + "/state", "{\"brightness\":" + fps + "}");
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					brightness = fps;
				}
			}

		});
		controlPanel.add(brightnessSlider);
		
		JColorChooser colorChooser = new JColorChooser();
		AbstractColorChooserPanel panels[] = colorChooser.getChooserPanels();
		for (int i = 0; i < 5; i ++) {
			if(!(panels[i].getDisplayName().equals("HSV")))
				colorChooser.removeChooserPanel(panels[i]);
		}
		colorChooser.setPreviewPanel(new JPanel());
		colorChooser.getSelectionModel().addChangeListener(new ChangeListener(){

			@Override
			public void stateChanged(ChangeEvent e) {
				int redColor = colorChooser.getColor().getRed();
				int greenColor = colorChooser.getColor().getGreen();
				int blueColor = colorChooser.getColor().getBlue();
				float[] hvs = new float[3];
				Color.RGBtoHSB(redColor, greenColor, blueColor, hvs);
				
				int hue = (int) (hvs[0] * 360);
				int sat = (int) (hvs[1]*10)*10;
				
				try {
					if(auroraCurEffect != "*Solid*"){
						putHTML(auroraUrl + "/effects", "{\"select\" : \"*Solid*\"}");
						putHTML(auroraUrl + "/state", "{\"hue\" : {\"value\":" + hue + "}}");
						putHTML(auroraUrl + "/state", "{\"sat\" : {\"value\":" + sat + "}}");
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			
		});
		
		controlPanel.add(colorChooser);

		controlFrame.add(controlPanel);
		controlFrame.pack();
		controlFrame.setVisible(true);
	}

	public static void main(String[] args) throws Exception {

		AuroraInfo();
		JFrameSetup();

		while (true) {
			JFrameUpdate();
			UpdateAuroraInfo();
		}
	}
}
