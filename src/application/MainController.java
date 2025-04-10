package application;

import javafx.fxml.FXML;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import application.lexer.Lexer;
import application.lexer.Token;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
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

	@FXML
	private Label encodingLabel;

	@FXML
	private Label languageLabel;

	@FXML
	private Button explorerMenuButton;

	@FXML
	private Button terminalMenuButton;

	@FXML
	private Button minimapMenuButton;
	
	private Map<Tab, File> tabFiles = new HashMap<>();
	private Map<Tab, TextArea> tabEditors = new HashMap<>();
	private PrintStream consoleStream;
	private long executionStartTime;

	@FXML
	public void initialize() {
	    setupContextMenus();
	    setupConsoleRedirect();
	    
	    // Adicionar listener para atualizar o label de linha/coluna quando a aba mudar
	    editorTabs.getSelectionModel().selectedItemProperty().addListener((_, _, newTab) -> {
	        if (newTab != null && tabEditors.containsKey(newTab)) {
	            updateLineColumnInfo(tabEditors.get(newTab));
	        }
	    });
	};
	
	private void setupContextMenus() {
		// Menu de contexto do Explorer
		ContextMenu explorerMenu = new ContextMenu();
		MenuItem refreshExplorer = new MenuItem("Atualizar Explorer");
		MenuItem collapseAll = new MenuItem("Recolher Tudo");
		MenuItem expandAll = new MenuItem("Expandir Tudo");
		
		refreshExplorer.setOnAction(_ -> {
			// Implementar atualização do explorer
			updateStatus("Explorer atualizado");
		});
		
		collapseAll.setOnAction(_ -> {
			// Implementar recolher todos os itens
			updateStatus("Todos os itens recolhidos");
		});
		
		expandAll.setOnAction(_ -> {
			// Implementar expandir todos os itens
			updateStatus("Todos os itens expandidos");
		});
		
		explorerMenu.getItems().addAll(refreshExplorer, collapseAll, expandAll);
		explorerMenuButton.setOnAction(_ -> explorerMenu.show(explorerMenuButton, explorerMenuButton.getLayoutX(), explorerMenuButton.getLayoutY()));

		// Menu de contexto do Terminal
		ContextMenu terminalMenu = new ContextMenu();
		MenuItem clearTerminal = new MenuItem("Limpar Terminal");
		MenuItem copyOutput = new MenuItem("Copiar Saída");
		MenuItem toggleWordWrap = new MenuItem("Alternar Quebra de Linha");
		
		clearTerminal.setOnAction(_ -> {
			saidaConsole.clear();
			updateStatus("Terminal limpo");
		});
		
		copyOutput.setOnAction(_ -> {
			// Implementar cópia da saída
			updateStatus("Saída copiada");
		});
		
		toggleWordWrap.setOnAction(_ -> {
			updateStatus("Quebra de linha " + (saidaConsole.isWrapText() ? "ativada" : "desativada"));
		});
		
		terminalMenu.getItems().addAll(clearTerminal, copyOutput, toggleWordWrap);
		terminalMenuButton.setOnAction(_ -> terminalMenu.show(terminalMenuButton, terminalMenuButton.getLayoutX(), terminalMenuButton.getLayoutY()));

		// Menu de contexto do Minimapa
		ContextMenu minimapMenu = new ContextMenu();
		MenuItem toggleMinimap = new MenuItem("Alternar Minimapa");
		MenuItem changeScale = new MenuItem("Alterar Escala");
		MenuItem showLineNumbers = new MenuItem("Mostrar Números de Linha");
		
		toggleMinimap.setOnAction(_ -> handleToggleMinimap());
		
		changeScale.setOnAction(_ -> {
			// Implementar mudança de escala
			updateStatus("Escala alterada");
		});
		
		showLineNumbers.setOnAction(_ -> {
			// Implementar mostrar/ocultar números de linha
			updateStatus("Ação realizada com sucesso");
		});
		
		minimapMenu.getItems().addAll(toggleMinimap, changeScale, showLineNumbers);
		minimapMenuButton.setOnAction(_ -> minimapMenu.show(minimapMenuButton, minimapMenuButton.getLayoutX(), minimapMenuButton.getLayoutY()));
	}
	
	private void setupConsoleRedirect() {
	    ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();
	    consoleStream = new PrintStream(consoleOutput) {
	        @Override
	        public void write(byte[] buf, int off, int len) {
	            super.write(buf, off, len);
	            String text = new String(buf, off, len);
	            Platform.runLater(() -> {
	                saidaConsole.appendText(text);
	            });
	        }
	        
	        @Override
	        public void write(int b) {
	            super.write(b);
	            char c = (char) b;
	            Platform.runLater(() -> {
	                saidaConsole.appendText(String.valueOf(c));
	            });
	        }
	    };
	    
	    // Redirecionar System.err
	    System.setErr(consoleStream);
	}
	
	// Menu File
	@FXML
	private void handleNewFile() {
		 TextInputDialog dialog = new TextInputDialog("NovoArquivo.scr");
		    dialog.setTitle("Novo Arquivo");
		    dialog.setHeaderText("Criar um novo arquivo");
		    dialog.setContentText("Nome do arquivo:");
		    
		    Optional<String> result = dialog.showAndWait();
		    result.ifPresent(fileName -> {
		        Tab newTab = new Tab(fileName);
		        TextArea codeEditor = createCodeEditor();
		        newTab.setContent(codeEditor);
		        editorTabs.getTabs().add(newTab);
		        editorTabs.getSelectionModel().select(newTab);
		        
		        tabEditors.put(newTab, codeEditor);
		        updateStatus("Novo arquivo criado: " + fileName);
		    });
	}

	@FXML
	private void handleOpenFile() {
		FileChooser fileChooser = new FileChooser();
	    fileChooser.setTitle("Abrir Arquivo");
	    fileChooser.getExtensionFilters().addAll(
	        new ExtensionFilter("Arquivos Scribo", "*.scr"),
	        new ExtensionFilter("Todos os Arquivos", "*.*")
	    );
	    
	    File file = fileChooser.showOpenDialog(getWindow());
	    if (file != null) {
	        openFile(file);
	    }
	}
	
	private void openFile(File file) {
	    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
	        StringBuilder content = new StringBuilder();
	        String line;
	        while ((line = reader.readLine()) != null) {
	            content.append(line).append("\n");
	        }
	        
	        Tab newTab = new Tab(file.getName());
	        TextArea codeEditor = createCodeEditor();
	        codeEditor.setText(content.toString());
	        newTab.setContent(codeEditor);
	        
	        editorTabs.getTabs().add(newTab);
	        editorTabs.getSelectionModel().select(newTab);
	        
	        tabFiles.put(newTab, file);
	        tabEditors.put(newTab, codeEditor);
	        
	        // Adicionar à lista de arquivos recentes
	        if (!recentFilesList.getItems().contains(file.getAbsolutePath())) {
	            recentFilesList.getItems().add(0, file.getAbsolutePath());
	        }
	        
	        updateStatus("Arquivo aberto: " + file.getName());
	    } catch (IOException e) {
	        showAlert(AlertType.ERROR, "Erro", "Erro ao abrir arquivo", 
	                 "Não foi possível abrir o arquivo: " + e.getMessage());
	    }
	}

	@FXML
	private void handleSaveFile() {
		 Tab currentTab = editorTabs.getSelectionModel().getSelectedItem();
		    if (currentTab == null) return;
		    
		    File file = tabFiles.get(currentTab);
		    if (file == null) {
		        handleSaveFileAs();
		    } else {
		        saveFile(currentTab, file);
		    }
	}

	@FXML
	private void handleSaveFileAs() {
		 Tab currentTab = editorTabs.getSelectionModel().getSelectedItem();
		    if (currentTab == null) return;
		    
		    FileChooser fileChooser = new FileChooser();
		    fileChooser.setTitle("Salvar Arquivo Como");
		    fileChooser.getExtensionFilters().addAll(
		        new ExtensionFilter("Arquivos Scribo", "*.scr"),
		        new ExtensionFilter("Todos os Arquivos", "*.*")
		    );
		    
		    File file = fileChooser.showSaveDialog(getWindow());
		    if (file != null) {
		        saveFile(currentTab, file);
		        currentTab.setText(file.getName());
		        tabFiles.put(currentTab, file);
		    }
	}
	
	private void saveFile(Tab tab, File file) {
	    TextArea codeEditor = tabEditors.get(tab);
	    if (codeEditor == null) return;
	    
	    try (FileWriter writer = new FileWriter(file)) {
	        writer.write(codeEditor.getText());
	        updateStatus("Arquivo salvo: " + file.getName());
	    } catch (IOException e) {
	        showAlert(AlertType.ERROR, "Erro", "Erro ao salvar arquivo", 
	                 "Não foi possível salvar o arquivo: " + e.getMessage());
	    }
	}

	@FXML
	private void handleExit() {
		getWindow().hide();
	}

	// Menu Edit
	@FXML
	private void handleUndo() {
		// Desfazer
	}

	@FXML
	private void handleRedo() {
		// Refazer
	}

	@FXML
	private void handleCut() {
		// Recortar
	}

	@FXML
	private void handleCopy() {
		// Copiar
	}

	@FXML
	private void handlePaste() {
		// Colar
	}

	@FXML
	private void handleFind() {
		// Localizar
	}

	@FXML
	private void handleReplace() {
		// Substituir
	}

	// Menu View
	@FXML
	private void handleShowTerminal() {
		consoleContainer.setVisible(true);
		updateStatus("Terminal visível");
	}

	@FXML
	private void handleHideTerminal() {
		consoleContainer.setVisible(false);
		updateStatus("Terminal oculto");
	}

	@FXML
	private void handleToggleSidebar() {
		// Alternar visibilidade da barra lateral
		if (fileExplorer.getScene() != null) {
			VBox sidebar = (VBox) fileExplorer.getScene().lookup(".sidebar-container");
			if (sidebar != null) {
				sidebar.setVisible(!sidebar.isVisible());
				updateStatus(sidebar.isVisible() ? "Barra lateral visível" : "Barra lateral oculta");
			}
		}
	}

	@FXML
	private void handleToggleMinimap() {
		// Alternar visibilidade do minimapa
		if (editorTabs.getScene() != null) {
			VBox minimap = (VBox) editorTabs.getScene().lookup(".minimap-container");
			if (minimap != null) {
				minimap.setVisible(!minimap.isVisible());
				updateStatus(minimap.isVisible() ? "Minimapa visível" : "Minimapa oculto");
			}
		}
	}

	// Menu Run
	@FXML
	private void handleExecutarCodigo() {
		  Tab currentTab = editorTabs.getSelectionModel().getSelectedItem();
		    if (currentTab == null) return;
		    
		    TextArea codeEditor = tabEditors.get(currentTab);
		    if (codeEditor == null) return;
		    
		    // Tornar o terminal visível se estiver oculto
		    if (!consoleContainer.isVisible()) {
		        consoleContainer.setVisible(true);
		        updateStatus("Terminal ativado");
		    }
		    
		    // Limpar o terminal antes de executar
		    saidaConsole.clear();
		    updateStatus("Executando código...");
		    executionStartTime = System.currentTimeMillis();
		    
		    // Executar o lexer em uma thread separada para não congelar a UI
		    new Thread(() -> {
		        try {
		            String code = codeEditor.getText();
		            Lexer lexer = new Lexer(code);
		            List<Token> tokens = lexer.scanTokens();
		            
		            // Calcular tempo de execução
		            long executionTime = System.currentTimeMillis() - executionStartTime;
		            
		            // Mostrar tokens no console
		            consoleStream.println("Análise léxica concluída em " + executionTime + "ms");
		            consoleStream.println("Total de tokens: " + tokens.size());
		            
		            if (!lexer.hadError()) {
		                consoleStream.println("\nTokens encontrados:");
		                for (Token token : tokens) {
		                    consoleStream.println(token);
		                }
		                
		                Platform.runLater(() -> {
		                    executionTimeLabel.setText(" | " + executionTime + "ms");
		                    updateStatus("Código executado com sucesso");
		                });
		            } else {
		                Platform.runLater(() -> {
		                    executionTimeLabel.setText(" | " + executionTime + "ms");
		                    updateStatus("Execução concluída com erros");
		                });
		            }
		        } catch (Exception e) {
		            StringWriter sw = new StringWriter();
		            e.printStackTrace(new PrintWriter(sw));
		            final String stackTrace = sw.toString();
		            
		            Platform.runLater(() -> {
		                consoleStream.println("Erro durante a execução:");
		                consoleStream.println(stackTrace);
		                updateStatus("Erro na execução do código");
		            });
		        }
		    }).start();
	}
	
	private TextArea createCodeEditor() {
	    TextArea codeEditor = new TextArea();
	    codeEditor.getStyleClass().add("code-editor");
	    
	    // Atualizar informações de linha/coluna
	    codeEditor.caretPositionProperty().addListener((_, _, _) -> {
	        updateLineColumnInfo(codeEditor);
	    });
	    
	    // Configurar indentação
	    codeEditor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
	        if (event.getCode() == KeyCode.TAB) {
	            codeEditor.insertText(codeEditor.getCaretPosition(), "    ");
	            event.consume();
	        }
	    });
	    
	    return codeEditor;
	}
	
	private void updateLineColumnInfo(TextArea editor) {
	    if (editor == null) return;
	    
	    int caretPosition = editor.getCaretPosition();
	    int line = 1;
	    int column = 1;
	    
	    String text = editor.getText();
	    for (int i = 0; i < caretPosition; i++) {
	        if (i < text.length() && text.charAt(i) == '\n') {
	            line++;
	            column = 1;
	        } else {
	            column++;
	        }
	    }
	    
	    lineColumnLabel.setText(" | Linha: " + line + ", Col: " + column);
	}

	@FXML
	private void handleDebug() {
		// Depurar código
	}

	@FXML
	private void handleStop() {
		// Parar execução
	}

	// Menu Help
	@FXML
	private void handleAbout() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Sobre o ScriboIDE");
		alert.setHeaderText("ScriboIDE v1.0");
		alert.setContentText("Um ambiente de desenvolvimento integrado simples e eficiente.\n\n" +
						   "Desenvolvido com JavaFX e Java.\n" +
						   "© 2024 - Todos os direitos reservados.");
		alert.showAndWait();
	}

	@FXML
	private void handleDocumentation() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Documentação");
		alert.setHeaderText("Documentação do ScriboIDE");
		alert.setContentText("A documentação completa está disponível em:\n" +
						   "https://github.com/seu-usuario/scriboide/wiki\n\n" +
						   "Recursos principais:\n" +
						   "- Editor de código com suporte a múltiplas abas\n" +
						   "- Terminal integrado\n" +
						   "- Explorador de arquivos\n" +
						   "- Minimapa para navegação\n" +
						   "- Suporte a múltiplas linguagens");
		alert.showAndWait();
	}

	// Outros
	@FXML
	private void handleClearRecentFiles() {
		 recentFilesList.getItems().clear();
		    updateStatus("Lista de arquivos recentes limpa");
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
		return statusLabel.getScene().getWindow();
	}
}