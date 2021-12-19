import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MainMenu extends JFrame implements WindowListener {

	private JPanel mainPanel;
	private JComboBox comboDevices;
	private JTextField path;
	private JTextArea logArea;
	private JScrollPane scroll;
	private JButton iniciarButton;
	private JProgressBar progressBar1;
	private Thread thread;
	private final File[] roots = File.listRoots();
	private static final StringBuilder missingList = new StringBuilder();

	public MainMenu(){
		initComponents();
		setTitle("Easy Game Transfer");
		setLocationRelativeTo(null);
		pack();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	private void initComponents() {
		mainPanel.setMinimumSize(new Dimension(800, 600));
		mainPanel.setMaximumSize(new Dimension(800, 600));
		mainPanel.setPreferredSize(new Dimension(800, 600));
		progressBar1.setVisible(false);
		progressBar1.setStringPainted(true);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addWindowListener(this);
		DefaultCaret caret = (DefaultCaret) logArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		getContentPane().add(mainPanel);

		for(File root : roots) {
			if(root.canRead() && root.canWrite()){
				comboDevices.addItem(root.getAbsolutePath());
			}
		}

		iniciarButton.addActionListener(v -> {
			int ans = JOptionPane.showOptionDialog(this,
			 "¿Seguro que desea copiar el juego al dispositivo " + comboDevices.getSelectedItem().toString() + "?",
				"¿Está seguro?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{"Sí", "No"}, null);

			if(ans == JOptionPane.NO_OPTION) {
				return;
			}

			if(path.getText() != null && !path.getText().trim().equalsIgnoreCase("")) {
				String pathS = path.getText();
				if(path.getText().indexOf("/") != -1) {
					pathS = path.getText().replace("/", "\\");
				}
				if(path.getText().substring(path.getText().length()-1).equalsIgnoreCase("\\")){
					pathS = path.getText().substring(0, path.getText().length() - 1);
				}
				String finalPathS = pathS;
				Runnable runnable = () -> {
					try {
						progressBar1.setVisible(true);
						iniciarButton.setEnabled(false);
						pasarJuego(finalPathS, comboDevices.getSelectedItem().toString());
						iniciarButton.setEnabled(true);
					} catch (IOException e) {
						e.printStackTrace();
					}
				};
				thread = new Thread(runnable);
				thread.start();
			} else {
				JOptionPane.showMessageDialog(this, "Debe ingresar un path del juego válido", "Error", JOptionPane.ERROR_MESSAGE);
			}
		});
	}

	public void pasarJuego(String basePath, String device) throws IOException {
		long startTimestamp = Timestamp.valueOf(LocalDateTime.now()).getTime();
		Map<String, Long> map = new HashMap<>();
		AtomicInteger fileAmount = new AtomicInteger();
		AtomicLong fullGameSize = new AtomicLong();

		try {
			Files.createFile(Path.of(basePath + "\\prueba.txt"));
			Files.delete(Path.of(basePath + "\\prueba.txt"));
		} catch (SecurityException e) {
			JOptionPane.showMessageDialog(this, "No se tiene permisos para escribir/leer en el dispositivo. Asegúrese de que se tengan los permisos y vuelva a intentar", "Error", JOptionPane.ERROR_MESSAGE);
			log("No se tiene permisos para escribir/leer en el dispositivo. Asegúrese de que se tengan los permisos y vuelva a intentar");
			return;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Ocurrió un problema al escribir en el dispositivo, por favor intente con otro dispositivo o más tarde", "Error", JOptionPane.ERROR_MESSAGE);
			log("Ocurrió un problema al escribir en el dispositivo, por favor intente con otro dispositivo o más tarde");
			return;
		}

		if(Files.exists(Paths.get(basePath + "\\missing.txt")) && Files.size(Paths.get(basePath + "\\missing.txt")) > 0) {
			log("Existe archivo \"missing.txt\". Procesando archivos faltantes");

			Scanner s = new Scanner(new File(basePath + "\\missing.txt"));
			while(s.hasNextLine()) {
				String line = s.nextLine();
				String fileName = line.split("\\|")[0];
				long fileSize = Long.parseLong(line.split("\\|")[1]);
				map.put(fileName, fileSize);
				fileAmount.addAndGet(1);
				fullGameSize.addAndGet(fileSize);
			}
			s.close();
		} else {
			log("No existe archivo \"missing.txt\". Procesando directorio completo");

			Files.walk(Paths.get(basePath)).filter(Files::isRegularFile).sorted(Comparator.comparing(Path::toString)).forEach(f -> {
				try {
					long size = Files.size(f);
					map.put(f.toString(), size);
					fileAmount.addAndGet(1);
					fullGameSize.addAndGet(size);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}

		log("Se van a transfererir " + fileAmount + " archivos con un peso total de " + fullGameSize.get() / 1000000000 + " GB");
		long initialSizeInDevice = new File(device).getFreeSpace();
		boolean isEnoughSpace = false;
		if(fullGameSize.get() < initialSizeInDevice) {
			isEnoughSpace = true;
		}
		boolean finalIsEnoughSpace = isEnoughSpace;
		map.forEach((k, v) -> {
			if(k.equals(basePath + "\\missing.txt")) return;
			long sizeInDevice = new File(device).getFreeSpace();
			log(sizeInDevice + " bytes restantes en el dispositivo");
			if(sizeInDevice < v) {
				log("No hay más espacio para " + k + " (" + v + "). Guardando en archivos restantes.");
				missingList.append(k).append("|").append(v).append("\n");
			} else {
				String pathS = k.replace(k.charAt(0) + ":\\", device);
				Path path = Paths.get(pathS.substring(0, pathS.lastIndexOf("\\")));
				try {
					if(!Files.exists(path)) {
						log("Creando path: " + path);
						Files.createDirectories(path);
					}
					log("Copiando " + v + " bytes de: " + k);
					long startFileTimestamp = Timestamp.valueOf(LocalDateTime.now()).getTime();
					try{
						Files.copy(Paths.get(k), Paths.get(pathS));
						sizeInDevice = new File(device).getFreeSpace();
						if(!finalIsEnoughSpace) {
							progressBar1.setValue((int) (100 - ((100 * sizeInDevice) / initialSizeInDevice)));
						} else {
							progressBar1.setValue((int) (100 - ((100 * (fullGameSize.intValue() - (initialSizeInDevice - sizeInDevice))) / fullGameSize.intValue())));
						}

					} catch (FileAlreadyExistsException e) {
						log("Ya existe el archivo " + pathS + " en el dispositivo destino. Salteando");
					} catch(IOException e) {
						log("Ocurrió un error al pasar el archivo " + pathS);
						e.printStackTrace();
					}
					long endFileTimestamp = Timestamp.valueOf(LocalDateTime.now()).getTime();
					log("Copiado ok: " + pathS + " en " + getTimestampElapsedTime(endFileTimestamp - startFileTimestamp));
				} catch (IOException e) {
					log("Error al copiar: " + pathS);
					e.printStackTrace();
				}
			}
		});

		Files.deleteIfExists(Paths.get(basePath + "\\missing.txt"));
		if(!missingList.toString().equals("")) {
			Files.createFile(Paths.get(basePath + "\\missing.txt"));
			log(getDate() + " - Escribiendo los archivos faltantes en disco para próxima ejecución");
			Files.writeString(Paths.get(basePath + "\\missing.txt"), missingList.toString());
		}

		long endTimestamp = Timestamp.valueOf(LocalDateTime.now()).getTime();
		if(!missingList.toString().equalsIgnoreCase("")) {
			JOptionPane.showMessageDialog(this, "Se terminó de transferir al dispositivo. Faltan transferir " + (missingList.toString().split("\\n").length - 1) + " archivos", "Ejecución finalizada", JOptionPane.INFORMATION_MESSAGE);
			log("Se terminó de transferir al dispositivo. Faltan transferir " + (missingList.toString().split("\n").length - 1) + " archivos");
		} else {
			JOptionPane.showMessageDialog(this, "Se terminó de transferir al dispositivo. No hay más archivos para transferir. Operacion completada", "Ejecución finalizada", JOptionPane.INFORMATION_MESSAGE);
			log("Se terminó de transferir al dispositivo. No hay más archivos para transferir. Operación completada");
		}

		log("Duración total de la transferencia: " + getTimestampElapsedTime(endTimestamp - startTimestamp));
	}

	private String getTimestampElapsedTime(long elapsedTime) {
		float time = (float) elapsedTime / 1000;
		if(time >= 60) {
			return (time / 60) + " minutos";
		} else {
			return time + " segundos";
		}
	}

	private void log(String texto) {
		System.out.println(getDate() + " - " + texto);
		logArea.append(getDate() + " - " + texto + "\n");
	}

	private String getDate() {
		LocalDateTime date = LocalDateTime.now();
		String dateS = date.toString();
		dateS = dateS.substring(8,10) + "/" + dateS.substring(5,7) + "/" + dateS.substring(0,4) + " - " + dateS.substring(11,19);
		return dateS;
	}

	@Override
	public void windowOpened(WindowEvent e) {}

	@Override
	public void windowClosing(WindowEvent e) {
		if(thread != null && thread.isAlive()) {
			thread.interrupt();
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowActivated(WindowEvent e) {}

	@Override
	public void windowDeactivated(WindowEvent e) {}
}
