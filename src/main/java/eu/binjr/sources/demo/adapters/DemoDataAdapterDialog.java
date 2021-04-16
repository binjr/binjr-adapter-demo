/*
 *    Copyright 2020 Frederic Thevenet
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

import eu.binjr.common.logging.Logger;
import eu.binjr.core.data.adapters.DataAdapter;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;
import eu.binjr.core.data.exceptions.DataAdapterException;
import eu.binjr.core.data.indexes.parser.profile.BuiltInParsingProfile;
import eu.binjr.core.dialogs.DataAdapterDialog;
import eu.binjr.core.dialogs.Dialogs;
import eu.binjr.sources.logs.adapters.LogsDataAdapter;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;

public class DemoDataAdapterDialog extends Dialog<Collection<DataAdapter>> {
    private static final Logger logger = Logger.create(DemoDataAdapterDialog.class);
    public static final String DEMO_DATA_LOGS = "/eu/binjr/demo/data/logs/";

    /**
     * Initializes a new instance of the {@link DataAdapterDialog} class.
     *
     * @param owner the owner window for the dialog
     */
    public DemoDataAdapterDialog(Node owner) {
        if (owner != null) {
            this.initOwner(Dialogs.getStage(owner));
        }
        this.setTitle("Source");

        var perfMonCheckbox = new CheckBox("Add a Demo JRDS source");
        perfMonCheckbox.setSelected(true);
        var logsCheckbox = new CheckBox("Add a Demo Log Files source");
        logsCheckbox.setSelected(true);
        var vBox = new VBox();
        vBox.setSpacing(10);
        vBox.getChildren().addAll(perfMonCheckbox, logsCheckbox);
        DialogPane dialogPane = new DialogPane();
        dialogPane.setHeaderText("Demo Data Adapter");
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialogPane.setGraphic(new Region());
        dialogPane.getGraphic().getStyleClass().addAll("source-icon", "dialog-icon");
        dialogPane.setContent(vBox);
        this.setDialogPane(dialogPane);

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        Collection<DataAdapter> result = new ArrayList<>();
        okButton.addEventFilter(ActionEvent.ACTION, ae -> {
            try {
                if (perfMonCheckbox.isSelected()) {
                    result.add(new DemoDataAdapter());
                }
                if (logsCheckbox.isSelected()) {
                    Path logPath;
                    final URI jarFileUri = getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
                    if (jarFileUri.toString().endsWith(".jar")) {
                        logPath = FileSystems.newFileSystem(Path.of(jarFileUri), (ClassLoader) null).getPath(DEMO_DATA_LOGS);
                        logger.debug(logPath.toString());
                    } else {
                        logPath = Path.of(getClass().getResource(DEMO_DATA_LOGS).toURI());
                    }
                    result.add(new LogsDataAdapter(
                            logPath,
                            ZoneId.systemDefault(),
                            new String[]{"*"},
                            new String[]{"*.log"},
                            BuiltInParsingProfile.BINJR_STRICT) );

                }
            } catch (CannotInitializeDataAdapterException e) {
                Dialogs.notifyException("Error initializing adapter to source", e, owner);
                ae.consume();
            } catch (DataAdapterException e) {
                Dialogs.notifyException("Error with the adapter to source", e, owner);
                ae.consume();
            } catch (Throwable e) {
                Dialogs.notifyException("Unexpected error while retrieving data adapter", e, owner);
                ae.consume();
            }
        });
        this.setResultConverter(dialogButton -> {
                    ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
                    if (data == ButtonBar.ButtonData.OK_DONE) {
                        return result;
                    }
                    return null;
                }
        );
    }

}
