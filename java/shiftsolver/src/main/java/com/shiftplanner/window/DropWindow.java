package com.shiftplanner.window;

import com.shiftplanner.domain.ShiftPlannerApp;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;


public class DropWindow extends JFrame {
    private final JLabel label;
    private final JButton cancelButton;
    private volatile boolean cancelRequested = false; // used if ShiftPlannerApp supports cancellation

    public DropWindow() {
        setTitle("Shift Solver");
        setSize(450, 160);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        label = new JLabel("Drop File here", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 16f));
        label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        label.setTransferHandler(new FileDropHandler());

        cancelButton = new JButton("Cancel");
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> {
            cancelButton.setEnabled(false);
            ShiftPlannerApp.requestTerminateEarly();
            // If ShiftPlannerApp exposes cancellation, call it here, e.g. ShiftPlannerApp.requestCancel();
        });

        add(label, BorderLayout.CENTER);
        add(cancelButton, BorderLayout.SOUTH);

        new DropTarget(label, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        Transferable t = dtde.getTransferable();
                        @SuppressWarnings("unchecked")
                        List<File> droppedFiles = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                        if (!droppedFiles.isEmpty()) {
                            File f = droppedFiles.get(0);
                            handleFileDrop(f.getAbsolutePath());
                        }
                        dtde.dropComplete(true);
                    } else {
                        dtde.rejectDrop();
                    }
                } catch (Exception ex) {
                    dtde.dropComplete(false);
                }
            }
        }, true, null);
    }

    private class FileDropHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;
            try {
                Transferable t = support.getTransferable();
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                if (!files.isEmpty()) {
                    File f = files.get(0);
                    handleFileDrop(f.getAbsolutePath());
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private void handleFileDrop(String path) {
        SwingUtilities.invokeLater(() -> {
            label.setText("Starting solver...");
            cancelButton.setEnabled(true);
            cancelRequested = false;
        });

        // Create a Score consumer that updates the label on EDT.
        Consumer<String> scoreConsumer = scoreText -> SwingUtilities.invokeLater(() -> label.setText(scoreText));

        new SwingWorker<Boolean, Void>() {
            private String runtimeErrorMessage = null;
            private volatile String lastScore = null;

            @Override
            protected Boolean doInBackground() {
                try {
                    java.util.function.Consumer<String> bestScoreConsumer = s ->{
                            lastScore = s;
                            SwingUtilities.invokeLater(() -> label.setText("Best score: " + s));};
                    return ShiftPlannerApp.solveShiftPlanFile(path, bestScoreConsumer);
                } catch (RuntimeException | Error e) {
                    // Record a concise message (type + message); keep stacktrace for logs
                    runtimeErrorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
                    e.printStackTrace();
                    return false;
                } catch (Exception e) {
                    runtimeErrorMessage = "Exception: " + e.getMessage();
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean result = get();
                    SwingUtilities.invokeLater(() -> {
                        if (runtimeErrorMessage != null) {
                            label.setText("<html><body style='text-align:center;color:red;'>Error:<br>" +
                                    escapeHtml4(runtimeErrorMessage) + "</body></html>");
                        } else if (result) {
                            label.setText("Finished! Last Score: "+lastScore);
                        } else {
                            label.setText("Cancelled or failed");
                        }
                        cancelButton.setEnabled(false);
                    });
                } catch (InterruptedException e) {
                    SwingUtilities.invokeLater(() -> label.setText("Interrupted"));
                } catch (ExecutionException e) {
                    // If the exception wasn't caught in doInBackground, unwrap and display it
                    Throwable cause = e.getCause();
                    String msg = cause != null ? (cause.getClass().getSimpleName() + ": " + cause.getMessage())
                            : e.getMessage();
                    final String display = msg;
                    SwingUtilities.invokeLater(() -> label.setText("<html><body style='text-align:center;color:red;'>Error:<br>" +
                            escapeHtml4(display) + "</body></html>"));
                }
            }
        }.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DropWindow w = new DropWindow();
            w.setVisible(true);
        });
    }
}
