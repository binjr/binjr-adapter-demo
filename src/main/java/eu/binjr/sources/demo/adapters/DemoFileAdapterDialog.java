/*
 *    Copyright 2019 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.binjr.sources.demo.adapters;

import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.dialogs.DataAdapterDialog;
import eu.binjr.core.dialogs.Dialogs;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * An implementation of the {@link DataAdapterDialog} class that presents a dialog box to retrieve the parameters specific {@link DemoDataAdapter}
 *
 * @author Frederic Thevenet
 */
public class DemoFileAdapterDialog extends DataAdapterDialog<Path> {
    private static final Logger logger = LogManager.getLogger(DemoFileAdapterDialog.class);
    private int pos = 2;

    /**
     * Initializes a new instance of the {@link DemoFileAdapterDialog} class.
     *
     * @param owner the owner window for the dialog
     */
    public DemoFileAdapterDialog(Node owner) {
        super(owner, Mode.PATH, "mostRecentDemoArchives");
        setDialogHeaderText("Add a Demo Archive or Folder");
    }

    @Override
    protected File displayFileChooser(Node owner) {
        try {
            ContextMenu sourceMenu = new ContextMenu();
            MenuItem menuItem = new MenuItem("Zip file");
            menuItem.setOnAction(eventHandler -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Open Demo Archive");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Demo archive", "*.zip"));
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
                setChooserInitialDir(fileChooser::setInitialDirectory);
                File selectedFile = fileChooser.showOpenDialog(Dialogs.getStage(owner));
                if (selectedFile != null) {
                    setSourceUri(selectedFile.getPath());
                }
            });
            sourceMenu.getItems().add(menuItem);
            MenuItem folderMenuItem = new MenuItem("Folder");
            folderMenuItem.setOnAction(eventHandler -> {
                DirectoryChooser dirChooser = new DirectoryChooser();
                dirChooser.setTitle("Open Demo Folder");
                setChooserInitialDir(dirChooser::setInitialDirectory);
                File selectedFile = dirChooser.showDialog(Dialogs.getStage(owner));
                if (selectedFile != null) {
                    setSourceUri(selectedFile.getPath());
                }
            });
            sourceMenu.getItems().add(folderMenuItem);
            sourceMenu.show(owner, Side.RIGHT, 0, 0);
        } catch (Exception e) {
            Dialogs.notifyException("Error while displaying file chooser: " + e.getMessage(), e, owner);
        }
        return null;
    }

    private void setChooserInitialDir(Consumer<File> dirSetter) {
        try {
            var initDir = getMostRecentList().peek().orElse(Paths.get(System.getProperty("user.home")));
            if (!Files.isDirectory(initDir) && initDir.getParent() != null) {
                initDir = initDir.getParent();
            }
            if (initDir.toRealPath().toFile().exists()) {
                dirSetter.accept(initDir.toFile());
            }
        } catch (Exception e) {
            logger.debug("Failed to set initial dir to file chooser", e);
        }
    }

    @Override
    protected DataAdapter getDataAdapter() throws DataAdapterException {
        Path archive = Paths.get(getSourceUri());
        if (!Files.exists(archive)) {
            throw new CannotInitializeDataAdapterException("Cannot find " + getSourceUri());
        }
        getMostRecentList().push(archive);
        return new DemoDataAdapter(archive);
    }
}
