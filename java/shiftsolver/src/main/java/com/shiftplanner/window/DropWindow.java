package com.shiftplanner.window;
import com.shiftplanner.domain.ShiftPlannerApp;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DropWindow extends JFrame {
    private final JLabel label;

    public DropWindow() {
        setTitle("Shift Solver");
        setSize(350, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        label = new JLabel("Drop File here", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 18f));
        label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        label.setTransferHandler(new FileDropHandler());

        add(label, BorderLayout.CENTER);

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
        SwingUtilities.invokeLater(() -> label.setText("calculating"));

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return ShiftPlannerApp.solveShiftPlanFile(path);
            }

            @Override
            protected void done() {
                try {
                    boolean result = get();
                    SwingUtilities.invokeLater(() -> {
                        if (result) {
                            label.setText("Success");
                        } else {
                            label.setText("Something went wrong");
                        }
                    });
                } catch (InterruptedException | ExecutionException e) {
                    SwingUtilities.invokeLater(() -> label.setText("Something went wrong"));
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