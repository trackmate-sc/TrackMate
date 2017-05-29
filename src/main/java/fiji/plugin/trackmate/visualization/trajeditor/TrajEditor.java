package fiji.plugin.trackmate.visualization.trajeditor;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javafx.application.Platform;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

public class TrajEditor extends AbstractTrackMateModelView {

    /*
     * CONSTANTS
     */
    public static final String INFO_TEXT = "<html>" + "TrajEditor is awesome." + "</html>";

    private static final Dimension DEFAULT_SIZE = new Dimension(1400, 860);

    public static final String KEY = "TRAJEDITOR";

    public static final ImageIcon TRACK_SCHEME_ICON = new ImageIcon(TrajEditor.class.getResource("/fiji/plugin/trackmate/visualization/trajeditor/icons/trajeditor.png"));
    public static final ImageIcon TRAJ_EDITOR_ICON_16X16;

    static {
        final Image image = TRACK_SCHEME_ICON.getImage();
        final Image newimg = image.getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH);
        TRAJ_EDITOR_ICON_16X16 = new ImageIcon(newimg);
    }

    /*
     * FIELDS
     */
    /**
     * The frame in which we display the TrackScheme GUI.
     */
    private TrajEditorFrame gui;

    /*
     * CONSTRUCTORS
     */
    public TrajEditor(final Model model, final SelectionModel selectionModel) {
        super(model, selectionModel);
        this.model.addModelChangeListener(this);
        initGUI();
    }

    private void initGUI() {
        this.gui = new TrajEditorFrame(this);

        gui.setSize(DEFAULT_SIZE);
        gui.setVisible(true);
        gui.setTitle("Trajectories Editor");
        gui.setIconImage(TRAJ_EDITOR_ICON_16X16.getImage());

        gui.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                model.removeModelChangeListener(TrajEditor.this);
            }
        });

    }

    @Override
    public void render() {

        Platform.runLater(() -> {
            
        });

    }

    @Override
    public void modelChanged(ModelChangeEvent event) {
        //System.out.println("Model changed in TrajEditor.");
    }

    @Override
    public void refresh() {
    }

    @Override
    public void clear() {
    }

    @Override
    public void centerViewOn(Spot spot) {
    }

    @Override
    public String getKey() {
        return KEY;
    }

    public JFrame getGUI() {
        return this.gui;
    }

}
