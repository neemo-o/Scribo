package application;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import application.interpreter.Interpretador;
import application.lexer.Lexer;
import application.lexer.Token;
import application.parser.Comando;
import application.parser.Parser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class MainController {

	@FXML
	private TabPane editorTabs;

	@FXML
	private TextArea saidaConsole;

	@FXML
	private VBox consoleContainer;

	@FXML
	private TreeView<String> fileExplorer;

	@FXML
	private ListView<String> recentFilesList;

	@FXML
	private Label statusLabel;

	@FXML
	private Label lineColumnLabel;

	@FXML
	private Label executionTimeLabel;

	private File currentWorkspace;
	private List<String> recentFiles = new ArrayList<>();
	private final int MAX_RECENT_FILES = 10;

	// Preferências para armazenar arquivos recentes
	private Preferences prefs1 = Preferences.userNodeForPackage(MainController.class);

	@FXML
	public void initialize() {
		// Configuração inicial do TreeView
		TreeItem<String> rootItem = new TreeItem<>("Workspace");
		rootItem.setExpanded(true);
		fileExplorer.setRoot(rootItem);

		// Carregar arquivos recentes
		loadRecentFiles();
		updateRecentFilesMenu();

		// Configurar ListView de arquivos recentes
		recentFilesList.getItems().addAll(recentFiles);
		recentFilesList.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2) {
				String selectedFile = recentFilesList.getSelectionModel().getSelectedItem();
				if (selectedFile != null) {
					openFile(new File(selectedFile));
				}
			}
		});

		// Configurar ação de duplo clique para abrir arquivo
		fileExplorer.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2) {
				TreeItem<String> selectedItem = fileExplorer.getSelectionModel().getSelectedItem();
				if (selectedItem != null && selectedItem.isLeaf() && !selectedItem.getValue().equals("Workspace")) {
					selectedItem.getValue();
					File file = new File(findFilePath(selectedItem));
					if (file.isFile()) {
						openFile(file);
					}
				}
			}
		});
	}

	private void loadRecentFiles() {
		recentFiles.clear();
		for (int i = 0; i < MAX_RECENT_FILES; i++) {
			String filePath = prefs1.get("recent_file_" + i, null);
			if (filePath != null) {
				recentFiles.add(filePath);
			}
		}
	}

	private void saveRecentFiles() {
		for (int i = 0; i < recentFiles.size() && i < MAX_RECENT_FILES; i++) {
			prefs1.put("recent_file_" + i, recentFiles.get(i));
		}

		// Limpar entradas antigas se houver menos arquivos recentes agora
		for (int i = recentFiles.size(); i < MAX_RECENT_FILES; i++) {
			prefs1.remove("recent_file_" + i);
		}
	}

	private void addRecentFile(String filePath) {
		// Remover duplicata se já existir
		recentFiles.remove(filePath);

		// Adicionar ao início da lista
		recentFiles.add(0, filePath);

		// Limitar tamanho da lista
		while (recentFiles.size() > MAX_RECENT_FILES) {
			recentFiles.remove(recentFiles.size() - 1);
		}

		// Atualizar UI e salvar
		updateRecentFilesUI();
		saveRecentFiles();
	}

	private void updateRecentFilesUI() {
		// Atualizar ListView
		recentFilesList.getItems().clear();
		recentFilesList.getItems().addAll(recentFiles);

		// Atualizar menu
		updateRecentFilesMenu();
	}

	private void updateRecentFilesMenu() {
		// Check if scene is ready
		if (editorTabs.getScene() == null) {
			// Scene isn't ready yet, we'll update the menu later
			Platform.runLater(this::updateRecentFilesMenu);
			return;
		}

		// Find the menu "Recent Files"
		try {
			for (javafx.scene.control.Menu menu : ((javafx.scene.control.MenuBar) ((VBox) ((BorderPane) editorTabs
					.getScene().getRoot()).getTop()).getChildren().get(0)).getMenus()) {
				if (menu.getText().equals("File")) {
					for (MenuItem item : menu.getItems()) {
						if (item instanceof Menu && ((Menu) item).getText().equals("Recent Files")) {
							Menu recentMenu = (Menu) item;

							// Limpar itens antigos, mas manter o "Clear Recent Files" e o separador
							while (recentMenu.getItems().size() > 2) {
								recentMenu.getItems().remove(recentMenu.getItems().size() - 1);
							}

							// Adicionar arquivos recentes
							for (String filePath : recentFiles) {
								File file = new File(filePath);
								MenuItem fileItem = new MenuItem(file.getName() + " - " + file.getParent());
								fileItem.setUserData(filePath);
								fileItem.setOnAction(_ -> openFile(new File((String) fileItem.getUserData())));
								recentMenu.getItems().add(fileItem);
							}
							break;
						}
					}
					break;
				}
			}
		} catch (Exception e) {
			// Handle possible NPE or other errors silently
			// We'll retry later when UI is fully initialized
			Platform.runLater(this::updateRecentFilesMenu);
		}
	}

	@FXML
	private void handleClearRecentFiles() {
		recentFiles.clear();
		updateRecentFilesUI();
		saveRecentFiles();
	}

	private String findFilePath(TreeItem<String> item) {
		StringBuilder path = new StringBuilder(item.getValue());
		TreeItem<String> parent = item.getParent();

		while (parent != null && !parent.getValue().equals("Workspace")) {
			path.insert(0, parent.getValue() + File.separator);
			parent = parent.getParent();
		}

		if (currentWorkspace != null) {
			return currentWorkspace.getAbsolutePath() + File.separator + path.toString();
		}
		return path.toString();
	}

	@FXML
	private void handleNewFile() {
		createNewTab("Untitled.src", "");
	}

	private void createNewTab(String title, String content) {
		Tab newTab = new Tab(title);

		// Criar o container para números de linha e editor
		HBox editorContainer = new HBox();
		editorContainer.getStyleClass().add("editor-with-line-numbers");

		// Área para números de linha
		VBox lineNumbers = new VBox();
		lineNumbers.getStyleClass().add("line-numbers");
		lineNumbers.setAlignment(Pos.TOP_RIGHT);

		// Editor de texto
		TextArea textArea = new TextArea(content);
		textArea.getStyleClass().add("code-editor");

		// Atualizar números de linha quando o texto mudar
		updateLineNumbers(lineNumbers, textArea);

		textArea.textProperty().addListener((_, _, _) -> {
			updateLineNumbers(lineNumbers, textArea);
			updateCursorPosition(textArea);

			// Marcar a aba como modificada se não for uma abertura inicial
			if (newTab.getUserData() != null) {
				if (!newTab.getText().endsWith("*")) {
					newTab.setText(newTab.getText() + "*");
				}
			}
		});

		// Atualizar posição do cursor
		textArea.caretPositionProperty().addListener((_, _, _) -> {
			updateCursorPosition(textArea);
		});

		// Manipular tecla Tab para indentação
		textArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.TAB) {
				event.consume();
				IndexRange selection = textArea.getSelection();
				textArea.insertText(selection.getStart(), "    ");
			}
		});

		// Adicionar componentes ao container
		editorContainer.getChildren().addAll(lineNumbers, textArea);
		HBox.setHgrow(textArea, Priority.ALWAYS);

		newTab.setContent(editorContainer);
		editorTabs.getTabs().add(newTab);
		editorTabs.getSelectionModel().select(newTab);
		updateStatus("New file created");
	}

	private void updateLineNumbers(VBox lineNumbers, TextArea textArea) {
		// Limpar números de linha existentes
		lineNumbers.getChildren().clear();

		// Contar linhas no texto
		String text = textArea.getText();
		int lineCount = text.isEmpty() ? 1 : text.split("\n", -1).length;

		// Adicionar números de linha
		for (int i = 1; i <= lineCount; i++) {
			Label lineNumber = new Label(Integer.toString(i));
			lineNumber.getStyleClass().add("line-number");
			lineNumber.setPadding(new Insets(0, 5, 0, 5));
			lineNumbers.getChildren().add(lineNumber);
		}
	}

	private void updateCursorPosition(TextArea textArea) {
		int caretPosition = textArea.getCaretPosition();
		String text = textArea.getText();

		// Calcular linha e coluna
		int line = 1;
		int column = 1;

		for (int i = 0; i < caretPosition; i++) {
			if (i < text.length() && text.charAt(i) == '\n') {
				line++;
				column = 1;
			} else {
				column++;
			}
		}

		lineColumnLabel.setText(" | Line: " + line + ", Col: " + column);
	}

	@FXML
	private void handleOpenFile() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open File");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Scribo Files", "*.src"),
				new FileChooser.ExtensionFilter("Text Files", "*.txt"),
				new FileChooser.ExtensionFilter("All Files", "*.*"));

		File file = fileChooser.showOpenDialog(getWindow());
		if (file != null) {
			openFile(file);
		}
	}

	private void openFile(File file) {
		try {
			String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

			// Verificar se o arquivo já está aberto
			for (Tab tab : editorTabs.getTabs()) {
				if (tab.getUserData() != null && tab.getUserData().equals(file.getAbsolutePath())) {
					editorTabs.getSelectionModel().select(tab);
					return;
				}
			}

			// Criar nova aba
			Tab newTab = new Tab(file.getName());

			// Criar o container para números de linha e editor
			HBox editorContainer = new HBox();
			editorContainer.getStyleClass().add("editor-with-line-numbers");

			// Área para números de linha
			VBox lineNumbers = new VBox();
			lineNumbers.getStyleClass().add("line-numbers");
			lineNumbers.setAlignment(Pos.TOP_RIGHT);

			// Editor de texto
			TextArea textArea = new TextArea(content);
			textArea.getStyleClass().add("code-editor");

			// Atualizar números de linha quando o texto mudar
			updateLineNumbers(lineNumbers, textArea);

			textArea.textProperty().addListener((_, _, _) -> {
				updateLineNumbers(lineNumbers, textArea);
				updateCursorPosition(textArea);

				// Marcar a aba como modificada
				if (!newTab.getText().endsWith("*")) {
					newTab.setText(newTab.getText() + "*");
				}
			});

			// Atualizar posição do cursor
			textArea.caretPositionProperty().addListener((_, _, _) -> {
				updateCursorPosition(textArea);
			});

			// Manipular tecla Tab para indentação
			textArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
				if (event.getCode() == KeyCode.TAB) {
					event.consume();
					IndexRange selection = textArea.getSelection();
					textArea.insertText(selection.getStart(), "    ");
				}
			});

			// Adicionar componentes ao container
			editorContainer.getChildren().addAll(lineNumbers, textArea);
			HBox.setHgrow(textArea, Priority.ALWAYS);

			newTab.setContent(editorContainer);
			newTab.setUserData(file.getAbsolutePath());
			editorTabs.getTabs().add(newTab);
			editorTabs.getSelectionModel().select(newTab);

			updateStatus("Opened: " + file.getAbsolutePath());

			// Adicionar à lista de arquivos recentes
			addRecentFile(file.getAbsolutePath());

		} catch (IOException e) {
			showAlert(AlertType.ERROR, "Error", "Could not open file", e.getMessage());
		}
	}

	@FXML
	private void handleSaveFile() {
		Tab selectedTab = editorTabs.getSelectionModel().getSelectedItem();
		if (selectedTab == null) {
			showAlert(AlertType.WARNING, "Warning", "No file to save", "Please open or create a file first.");
			return;
		}

		// Verificar se já tem um arquivo associado
		if (selectedTab.getUserData() != null) {
			saveToFile(new File(selectedTab.getUserData().toString()), selectedTab);
		} else {
			handleSaveFileAs();
		}
	}

	@FXML
	private void handleSaveFileAs() {
		Tab selectedTab = editorTabs.getSelectionModel().getSelectedItem();
		if (selectedTab == null) {
			showAlert(AlertType.WARNING, "Warning", "No file to save", "Please open or create a file first.");
			return;
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save File");
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Scribo Files", "*.src"),
				new FileChooser.ExtensionFilter("Text Files", "*.txt"));

		File file = fileChooser.showSaveDialog(getWindow());
		if (file != null) {
			saveToFile(file, selectedTab);
		}
	}

	private void saveToFile(File file, Tab tab) {
		try {
			// Obter o TextArea do container HBox
			HBox container = (HBox) tab.getContent();
			TextArea textArea = (TextArea) container.getChildren().get(1);

			Files.write(file.toPath(), textArea.getText().getBytes(StandardCharsets.UTF_8));
			tab.setText(file.getName());
			tab.setUserData(file.getAbsolutePath());
			updateStatus("Saved: " + file.getAbsolutePath());

			// Adicionar à lista de arquivos recentes
			addRecentFile(file.getAbsolutePath());

		} catch (IOException e) {
			showAlert(AlertType.ERROR, "Error", "Could not save file", e.getMessage());
		}
	}

	@FXML
	private void handleExit() {
		// Verificar se há arquivos não salvos
		for (Tab tab : editorTabs.getTabs()) {
			if (tab.getText().contains("*")) {
				Alert alert = new Alert(AlertType.CONFIRMATION);
				alert.setTitle("Unsaved Changes");
				alert.setHeaderText("There are unsaved changes.");
				alert.setContentText("Do you want to save before exiting?");

				ButtonType buttonSave = new ButtonType("Save");
				ButtonType buttonDontSave = new ButtonType("Don't Save");
				ButtonType buttonCancel = new ButtonType("Cancel");

				alert.getButtonTypes().setAll(buttonSave, buttonDontSave, buttonCancel);

				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == buttonSave) {
					editorTabs.getSelectionModel().select(tab);
					handleSaveFile();
				} else if (result.get() == buttonCancel) {
					return;
				}
			}
		}

		System.exit(0);
	}

	@FXML
	private void handleShowTerminal() {
		consoleContainer.setVisible(true);
		updateStatus("Terminal shown");
	}

	@FXML
	private void handleHideTerminal() {
		consoleContainer.setVisible(false);
		updateStatus("Terminal hidden");
	}

	@FXML
	private void handleAbout() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About Scribo Studio");
		alert.setHeaderText("Scribo Studio");
		alert.setContentText("Uma IDE para a linguagem Scribo.\nVersão 1.0");
		alert.showAndWait();
	}

	@FXML
	private void handleExecutarCodigo() {
		// Verifica se há uma aba selecionada
		Tab selectedTab = editorTabs.getSelectionModel().getSelectedItem();
		if (selectedTab != null) {
			// Obter o TextArea do container HBox
			HBox container = (HBox) selectedTab.getContent();
			TextArea editorCodigo = (TextArea) container.getChildren().get(1);
			String codigo = editorCodigo.getText();
			saidaConsole.clear();

			// Mostrar o terminal se estiver escondido
			if (!consoleContainer.isVisible()) {
				consoleContainer.setVisible(true);
			}

			// Registrar tempo de início
			long startTime = System.currentTimeMillis();

			try {
				Lexer lexer = new Lexer(codigo);
				List<Token> tokens = lexer.escanearTokens();

				Parser parser = new Parser(tokens);
				List<Comando> comandos = parser.parse();

				Interpretador interpretador = new Interpretador(msg -> saidaConsole.appendText(msg + "\n"));
				interpretador.interpretar(comandos);

				// Calcular tempo de execução
				long executionTime = System.currentTimeMillis() - startTime;

				// Atualizar label de tempo de execução
				executionTimeLabel.setText(" | Tempo: " + executionTime + "ms");

				// Mensagem de finalização do processo
				saidaConsole.appendText(
						"[Info] Processo concluído -> ACABOU TUDO. (Executado em " + executionTime + "ms)\n");
			} catch (Exception e) {
				// Calcular tempo de execução mesmo em caso de erro
				long executionTime = System.currentTimeMillis() - startTime;
				executionTimeLabel.setText("Tempo: " + executionTime + "ms");

				saidaConsole.appendText("[Erro] " + e.getMessage() + "\n");
			}
		} else {
			saidaConsole.appendText("[Erro] Nenhuma aba selecionada.\n");
		}
	}

	private void showAlert(AlertType type, String title, String header, String content) {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.showAndWait();
	}

	private void updateStatus(String status) {
		statusLabel.setText(status);
	}

	@FXML
	private Window getWindow() {
		return editorTabs.getScene().getWindow();
	}
}