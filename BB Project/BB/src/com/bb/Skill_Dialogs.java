package com.bb;

import skills.Skills;
import skills.SkillsRegistry;
import skills.Skills_Register;


import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class Skill_Dialogs extends JPanel {


    private final List<JToggleButton> buttons = new ArrayList<>();
    private final Map<JToggleButton, Skills> skills = new HashMap<>();

    private final CardLayout cl;
    private final JPanel cards;
    private final Consumer<List<Skills>> onConfirmSkills; // callback to hand selected skills to caller

    public Skill_Dialogs(CardLayout cl, JPanel cards, Consumer<List<Skills>> onConfirmSkills) {
        this.cl = cl;
        this.cards = cards;
        this.onConfirmSkills = onConfirmSkills;

        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel grid = new JPanel();
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(50, 30, 50, 30));
        grid.setLayout(new FlowLayout(FlowLayout.CENTER, 12, 12));

        JLabel title = new JLabel("Select 2 Skills", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(Color.WHITE); // Make text white to be visible on background
        add(title, BorderLayout.NORTH);

        List<Skills> allSkills = Skills_Register.getAllSkills();

        for (Skills s : allSkills) {
            String imagePath = s.getImage();
            ImageIcon icon = loadIcon(imagePath, 180, 180);
            ImageIcon fadeIcon = Fade(icon, 0.45f);

            JToggleButton btn = new JToggleButton();
            if (icon != null) {
                btn.setIcon(icon);
            } else {
                // Missing art must not make the skill unpickable, so fall back to its name.
                btn.setText("<html><div style='text-align:center'>" + s.getName() + "</div></html>");
            }
            btn.setPreferredSize(new Dimension(180, 180));
            btn.setToolTipText(s.getDescription());
            btn.setFocusPainted(false);

            btn.addItemListener(e -> {
                if (fadeIcon != null) btn.setSelectedIcon(fadeIcon);

                long selectedCount = buttons.stream().filter(AbstractButton::isSelected).count();
                if (btn.isSelected() && selectedCount > 2) {
                    btn.setSelected(false);
                    Toolkit.getDefaultToolkit().beep();
                }
            });

            // store mapping and add to grid
            buttons.add(btn);
            skills.put(btn, s);

            // show icon and a small label under it
            JPanel cell = new JPanel(new BorderLayout());
            cell.setOpaque(false);
            cell.add(btn, BorderLayout.CENTER);
            JLabel desc = new JLabel(s.getName(), SwingConstants.CENTER);
            desc.setFont(desc.getFont().deriveFont(12f));
            desc.setForeground(Color.WHITE); // Make text white
            cell.add(desc, BorderLayout.SOUTH);
            grid.add(cell);
        }

        JButton confirm = new JButton("Confirm Selection");
        confirm.setPreferredSize(new Dimension(200, 40));
        confirm.addActionListener(e -> onConfirm());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        bottom.add(confirm);

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void onConfirm() {
        List<Skills> selected = getSelectedSkills();
        if (selected.size() != 2) {
            JOptionPane.showMessageDialog(
                    this,
                    "You must select exactly 2 skills.",
                    "Invalid selection",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // hand selected skills to caller (e.g., Frames will call player.setActiveSkills(...))
        if (onConfirmSkills != null) {
            onConfirmSkills.accept(selected);
        }
        SkillsRegistry.setSelectedSkills(selected);

        // switch to player card
        cl.show(cards, "PLAYER");
    }

    /** Unticks every skill so a new run starts from a clean picker. */
    public void clearSelection() {
        for (JToggleButton btn : buttons) {
            btn.setSelected(false);
        }
    }

    public List<Skills> getSelectedSkills() {
        List<Skills> selected = new ArrayList<>();
        for (JToggleButton btn : buttons) {
            if (btn.isSelected()) {
                selected.add(skills.get(btn));
            }
        }
        return selected;
    }

    /** Loads and scales a skill icon, or returns null when the art is missing. */
    private ImageIcon loadIcon(String resourcePath, int width, int height) {
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        java.net.URL url = Assets.getResource(path);
        if (url == null) {
            System.err.println("Skill icon not found on the classpath: " + path);
            return null;
        }
        ImageIcon raw = new ImageIcon(url);
        if (raw.getImage() == null || raw.getIconWidth() <= 0) {
            System.err.println("Skill icon could not be decoded: " + path);
            return null;
        }
        Image scaled = raw.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    /**
     * Returns a dimmed copy used as the "selected" icon.
     *
     * <p>Returns null for a null input: previously a missing icon reached here as an empty
     * {@code ImageIcon} whose backing image is null, and dereferencing it threw an NPE that
     * killed the whole window during construction.
     */
    private ImageIcon Fade(ImageIcon original, float brightnessFactor) {
        if (original == null) return null;
        Image img = original.getImage();
        if (img == null || img.getWidth(null) <= 0 || img.getHeight(null) <= 0) return null;
        BufferedImage buffered = new BufferedImage(
                img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = buffered.createGraphics();
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();

        RescaleOp op = new RescaleOp(
                new float[]{brightnessFactor, brightnessFactor, brightnessFactor, 1f},
                new float[]{0, 0, 0, 0}, null);

        BufferedImage darkened = op.filter(buffered, null);
        return new ImageIcon(darkened);
    }

}

