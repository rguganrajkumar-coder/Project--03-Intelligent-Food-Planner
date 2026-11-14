import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * FOODVERSE - Smart Food Planner (modern Swing UI)
 * Compile: javac -encoding UTF-8 FoodVerse.java
 * Run:     java FoodVerse
 */
public class FoodVerse extends JFrame {

    // ===================== THEME =====================
    static final Color PRIMARY      = new Color(22, 163, 74);
    static final Color PRIMARY_DARK = new Color(21, 128, 61);
    static final Color DEEP         = new Color(20, 83, 45);
    static final Color ACCENT       = new Color(249, 115, 22);
    static final Color DANGER       = new Color(220, 38, 38);
    static final Color BG           = new Color(243, 248, 243);
    static final Color SOFT         = new Color(220, 252, 231);
    static final Color TEXT         = new Color(31, 41, 55);
    static final Color MUTED        = new Color(107, 114, 128);
    static final Color LINE         = new Color(226, 232, 226);
    static final Color GHOST        = new Color(255, 255, 255, 45);

    static final String RS = "\u20B9"; // rupee sign
    static final String FAMILY = pickFamily(new String[]{
        "Segoe UI", "Poppins", "Helvetica Neue", "Ubuntu", "Noto Sans"}, Font.SANS_SERIF);
    static final String EMOJI_FAMILY = pickFamily(new String[]{
        "Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji"}, Font.DIALOG);

    static String pickFamily(String[] wanted, String fallback) {
        Set<String> names = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String w : wanted) if (names.contains(w)) return w;
        return fallback;
    }

    static Font font(int style, int size) { return new Font(FAMILY, style, size); }
    static Font emojiFont(int size)       { return new Font(EMOJI_FAMILY, Font.PLAIN, size); }

    static Color mix(Color a, Color b, double t) {
        return new Color(
            (int) Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t),
            (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t),
            a.getAlpha());
    }

    static void smooth(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    static String emoji(String category) {
        switch (category) {
            case "Breakfast": return "\uD83E\uDD5E"; // pancakes
            case "Lunch":     return "\uD83C\uDF5B"; // curry
            case "Snack":     return "\uD83E\uDD57"; // salad
            default:          return "\uD83C\uDF5D"; // pasta
        }
    }

    static JLabel label(String text, Font f, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(f);
        l.setForeground(c);
        return l;
    }

    // ===================== CUSTOM COMPONENTS =====================

    /** Rounded button with smooth hover animation and press effect. */
    static class RoundedButton extends JButton {
        private final Color base;
        private float hoverAmt = 0f;
        private boolean hovering, down;
        private int arc = 26;
        private final Timer anim;

        RoundedButton(String text, Color base) {
            super(text);
            this.base = base;
            setFont(font(Font.BOLD, 13));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 20, 13, 20));

            anim = new Timer(15, e -> {
                float target = hovering ? 1f : 0f;
                hoverAmt += (target - hoverAmt) * 0.35f;
                if (Math.abs(target - hoverAmt) < 0.02f) {
                    hoverAmt = target;
                    ((Timer) e.getSource()).stop();
                }
                repaint();
            });

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovering = true; anim.start(); }
                public void mouseExited(MouseEvent e)  { hovering = false; down = false; anim.start(); }
                public void mousePressed(MouseEvent e) { down = true; repaint(); }
                public void mouseReleased(MouseEvent e){ down = false; repaint(); }
            });
        }

        RoundedButton small() {
            setFont(font(Font.BOLD, 12));
            setBorder(new EmptyBorder(5, 12, 8, 12));
            arc = 20;
            return this;
        }

        RoundedButton text(Color c) { setForeground(c); return this; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth(), h = getHeight();
            Color c = mix(base, Color.WHITE, 0.16 * hoverAmt);
            if (down) c = mix(base, Color.BLACK, 0.15);
            if (!isEnabled()) c = new Color(200, 205, 200);
            int off = down ? 1 : 0;
            g2.setColor(new Color(0, 0, 0, (int) (28 * base.getAlpha() / 255f)));
            g2.fillRoundRect(1, 3, w - 2, h - 3, arc, arc);
            g2.setColor(c);
            g2.fillRoundRect(0, off, w, h - 3, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Sidebar navigation button. */
    static class NavButton extends JButton {
        private boolean active, over;
        private final String baseText;

        NavButton(String icon, String text) {
            super(icon + "   " + text);
            baseText = icon + "   " + text;
            setFont(font(Font.BOLD, 14));
            setForeground(new Color(209, 250, 229));
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(12, 20, 12, 12));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { over = true; repaint(); }
                public void mouseExited(MouseEvent e)  { over = false; repaint(); }
            });
        }

        void setActive(boolean a) {
            active = a;
            setForeground(a ? Color.WHITE : new Color(209, 250, 229));
            repaint();
        }

        void setBadge(int n) { setText(n > 0 ? baseText + "   (" + n + ")" : baseText); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth(), h = getHeight();
            if (active) {
                g2.setColor(new Color(255, 255, 255, 45));
                g2.fillRoundRect(0, 0, w, h, 18, 18);
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, 11, 5, h - 22, 5, 5);
            } else if (over) {
                g2.setColor(new Color(255, 255, 255, 22));
                g2.fillRoundRect(0, 0, w, h, 18, 18);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** White rounded card with a soft shadow. */
    static class Card extends JPanel {
        Card(LayoutManager lm) {
            super(lm);
            setOpaque(false);
            setBorder(new EmptyBorder(18, 22, 24, 22));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth(), h = getHeight();
            for (int i = 1; i <= 5; i++) {
                g2.setColor(new Color(16, 60, 30, 9));
                g2.fillRoundRect(4 - i, 5 - i, w - 8 + 2 * i, h - 8 + 2 * i, 24 + i, 24 + i);
            }
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(4, 4, w - 8, h - 8, 24, 24);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Gradient panel (optionally rounded) with soft decorative circles. */
    static class Gradient extends JPanel {
        private final Color c1, c2;
        private final int arc;
        private final boolean vertical;

        Gradient(Color c1, Color c2, int arc, boolean vertical) {
            this.c1 = c1; this.c2 = c2; this.arc = arc; this.vertical = vertical;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth(), h = getHeight();
            if (arc > 0) g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));
            g2.setPaint(vertical ? new GradientPaint(0, 0, c1, 0, h, c2)
                                 : new GradientPaint(0, 0, c1, w, h, c2));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(255, 255, 255, 18));
            g2.fillOval(w - 170, -70, 280, 280);
            g2.fillOval(-60, h - 100, 180, 180);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Rounded text field with focus ring and placeholder text. */
    static class RoundField extends JTextField {
        private final String hint;

        RoundField(int cols, String hint) {
            super(cols);
            this.hint = hint;
            setOpaque(false);
            setFont(font(Font.PLAIN, 14));
            setForeground(TEXT);
            setCaretColor(PRIMARY_DARK);
            setBorder(new EmptyBorder(10, 16, 10, 16));
            addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { repaint(); }
                public void focusLost(FocusEvent e)   { repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 26, 26);
            g2.setColor(isFocusOwner() ? PRIMARY : LINE);
            g2.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1.2f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 26, 26);
            if (getText().isEmpty()) {
                g2.setColor(new Color(156, 163, 175));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(hint, getInsets().left, (h - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Thin rounded scrollbar. */
    static class ModernScrollBar extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() {
            thumbColor = new Color(180, 210, 188);
            trackColor = Color.WHITE;
        }
        private JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            return b;
        }
        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(trackColor);
            g.fillRect(r.x, r.y, r.width, r.height);
        }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(isThumbRollover() ? PRIMARY : thumbColor);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }
    }

    static void styleScroll(JScrollPane sp) {
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUI(new ModernScrollBar());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        sp.getVerticalScrollBar().setUnitIncrement(16);
    }

    static void styleCombo(JComboBox<String> box) {
        box.setFont(font(Font.PLAIN, 14));
        box.setBackground(Color.WHITE);
        box.setForeground(TEXT);
        box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        box.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton b = new JButton("\u25BE");
                b.setBorder(new EmptyBorder(0, 8, 0, 10));
                b.setContentAreaFilled(false);
                b.setFocusPainted(false);
                b.setForeground(PRIMARY_DARK);
                return b;
            }
        });
        box.setBorder(new CompoundBorder(new LineBorder(LINE, 1, true), new EmptyBorder(4, 8, 4, 0)));
        box.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                setBorder(new EmptyBorder(7, 10, 7, 10));
                setFont(font(Font.PLAIN, 14));
                setBackground(sel ? SOFT : Color.WHITE);
                setForeground(sel ? DEEP : TEXT);
                return this;
            }
        });
    }

    // ===================== DATA MODEL =====================
    static class Food {
        String name, category, cuisine;
        String[] ingredients, steps;
        int homePrice, onlinePrice;

        Food(String name, String category, String cuisine,
             String[] ingredients, String[] steps,
             int homePrice, int onlinePrice) {
            this.name = name;
            this.category = category;
            this.cuisine = cuisine;
            this.ingredients = ingredients;
            this.steps = steps;
            this.homePrice = homePrice;
            this.onlinePrice = onlinePrice;
        }
    }

    // ===================== STATE =====================
    List<Food> foods = new ArrayList<>();
    Map<String, Integer> cart = new LinkedHashMap<>();
    Map<String, NavButton> navs = new LinkedHashMap<>();

    CardLayout cardLayout = new CardLayout();
    JPanel pages = new JPanel(cardLayout);

    JLabel statusText = new JLabel();
    Timer statusTimer;

    // discover
    DefaultListModel<String> foodModel = new DefaultListModel<>();
    JList<String> foodList = new JList<>(foodModel);
    JEditorPane details = new JEditorPane("text/html", "");
    RoundField searchField = new RoundField(18, "Search recipes...");
    JComboBox<String> categoryBox = new JComboBox<>(
        new String[]{"All", "Breakfast", "Lunch", "Snack", "Dinner"});

    // planner
    static final String[] MEALS = {"Breakfast", "Lunch", "Snack", "Dinner"};
    Food[] plan = new Food[4];
    JLabel[] planName = new JLabel[4];
    JLabel[] planCost = new JLabel[4];
    JLabel planSummary = new JLabel();

    // maker
    RoundField ingredientField = new RoundField(26, "e.g. tomato, onion, rice");
    JEditorPane makerOutput = new JEditorPane("text/html", "");

    // compare
    PriceChart chart;

    // cart
    JPanel cartRows = new JPanel();
    JLabel cartTotal = new JLabel();

    public FoodVerse() {
        setTitle("FOODVERSE - Smart Food Planner");
        setSize(1200, 760);
        setMinimumSize(new Dimension(1020, 660));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        loadFoods();
        createInterface();
    }

    // ===================== FOOD DATABASE =====================
    void loadFoods() {
        foods.add(new Food("Idli Sambar", "Breakfast", "Indian",
            new String[]{"Idli batter", "Toor dal", "Tomato", "Onion"},
            new String[]{"Prepare idli batter.", "Steam idlis until cooked.",
                         "Cook dal with tomato and spices.", "Serve hot with sambar."}, 40, 90));

        foods.add(new Food("Masala Dosa", "Breakfast", "Indian",
            new String[]{"Rice batter", "Potato", "Onion", "Spices"},
            new String[]{"Prepare dosa batter.", "Cook potato masala.",
                         "Spread batter on a pan.", "Add masala and fold."}, 50, 100));

        foods.add(new Food("Vegetable Poha", "Breakfast", "Indian",
            new String[]{"Poha", "Onion", "Peanuts", "Lemon"},
            new String[]{"Rinse poha gently.", "Saute onion and peanuts.",
                         "Add poha and spices.", "Finish with lemon juice."}, 30, 80));

        foods.add(new Food("Dal Rice", "Lunch", "Indian",
            new String[]{"Rice", "Toor dal", "Tomato", "Onion"},
            new String[]{"Cook rice.", "Cook dal with tomato.",
                         "Prepare onion seasoning.", "Serve together."}, 35, 120));

        foods.add(new Food("Paneer Wrap", "Lunch", "Indian",
            new String[]{"Paneer", "Chapati", "Onion", "Capsicum"},
            new String[]{"Cut paneer into cubes.", "Saute vegetables.",
                         "Add paneer and spices.", "Fill chapati and roll."}, 65, 150));

        foods.add(new Food("Chicken Curry Rice", "Lunch", "Indian",
            new String[]{"Chicken", "Rice", "Tomato", "Onion"},
            new String[]{"Prepare onion and tomato masala.", "Add chicken and cook thoroughly.",
                         "Cook rice separately.", "Serve together."}, 100, 250));

        foods.add(new Food("Chickpea Salad", "Snack", "Mediterranean",
            new String[]{"Chickpeas", "Cucumber", "Tomato", "Lemon"},
            new String[]{"Cook chickpeas until tender.", "Chop vegetables.",
                         "Mix all ingredients.", "Add lemon juice."}, 45, 130));

        foods.add(new Food("Veg Fried Rice", "Dinner", "Chinese",
            new String[]{"Rice", "Carrot", "Beans", "Onion"},
            new String[]{"Cook rice.", "Chop vegetables.",
                         "Saute vegetables.", "Add rice and mix well."}, 55, 140));

        foods.add(new Food("Pasta", "Dinner", "Italian",
            new String[]{"Pasta", "Tomato", "Onion", "Cheese"},
            new String[]{"Boil pasta.", "Prepare tomato sauce.",
                         "Mix pasta with sauce.", "Add cheese and serve."}, 70, 180));

        foods.add(new Food("Banana Smoothie", "Breakfast", "American",
            new String[]{"Banana", "Milk", "Oats", "Honey"},
            new String[]{"Add banana and milk to a blender.", "Add oats and honey.",
                         "Blend until smooth.", "Serve chilled."}, 35, 100));
    }

    // ===================== MAIN LAYOUT =====================
    void createInterface() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        setContentPane(root);

        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);

        pages.setOpaque(false);
        pages.add(homePage(), "home");
        pages.add(discoverPage(), "discover");
        pages.add(plannerPage(), "planner");
        pages.add(makerPage(), "maker");
        pages.add(comparePage(), "compare");
        pages.add(cartPage(), "cart");

        right.add(pages, BorderLayout.CENTER);
        right.add(buildStatusBar(), BorderLayout.SOUTH);
        root.add(right, BorderLayout.CENTER);

        showPage("home");
        setStatus("Welcome to FOODVERSE!");
    }

    JPanel buildSidebar() {
        Gradient side = new Gradient(DEEP, new Color(22, 101, 52), 0, true);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(250, 0));
        side.setBorder(new EmptyBorder(28, 18, 20, 18));

        JLabel logo = new JLabel(
            "<html><span style='color:white'>FOOD</span><span style='color:#fb923c'>VERSE</span></html>");
        logo.setFont(font(Font.BOLD, 28));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        logo.setBorder(new EmptyBorder(0, 6, 0, 0));

        JLabel tag = label("Eat Smart  \u2022  Save More", font(Font.PLAIN, 12), new Color(167, 243, 208));
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);
        tag.setBorder(new EmptyBorder(2, 8, 0, 0));

        side.add(logo);
        side.add(tag);
        side.add(Box.createVerticalStrut(32));

        addNav(side, "home",     "\uD83C\uDFE0", "Home");
        addNav(side, "discover", "\uD83D\uDD0D", "Discover");
        addNav(side, "planner",  "\uD83D\uDCC5", "Meal Planner");
        addNav(side, "maker",    "\uD83C\uDF73", "Food Maker");
        addNav(side, "compare",  "\uD83D\uDCB0", "Price Compare");
        addNav(side, "cart",     "\uD83D\uDED2", "Grocery Cart");

        side.add(Box.createVerticalGlue());

        JLabel foot = label("Prices in INR (" + RS + ")  \u2022  v2.0", font(Font.PLAIN, 11), new Color(134, 239, 172));
        foot.setAlignmentX(Component.LEFT_ALIGNMENT);
        foot.setBorder(new EmptyBorder(0, 8, 0, 0));
        side.add(foot);

        return side;
    }

    void addNav(JPanel side, String key, String icon, String text) {
        NavButton b = new NavButton(icon, text);
        b.addActionListener(e -> showPage(key));
        navs.put(key, b);
        side.add(b);
        side.add(Box.createVerticalStrut(6));
    }

    void showPage(String key) {
        cardLayout.show(pages, key);
        for (Map.Entry<String, NavButton> en : navs.entrySet()) {
            en.getValue().setActive(en.getKey().equals(key));
        }
    }

    JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, LINE),
                                         new EmptyBorder(9, 28, 9, 28)));
        statusText.setFont(font(Font.PLAIN, 13));
        statusText.setForeground(PRIMARY_DARK);
        bar.add(statusText, BorderLayout.WEST);
        return bar;
    }

    void setStatus(String msg) {
        statusText.setText("\u25CF  " + msg);
        if (statusTimer != null) statusTimer.stop();
        statusTimer = new Timer(3500, e -> statusText.setText("\u25CF  Ready"));
        statusTimer.setRepeats(false);
        statusTimer.start();
    }

    // ===================== LAYOUT HELPERS =====================
    JPanel page() {
        JPanel p = new JPanel(new BorderLayout(0, 18));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(28, 32, 16, 32));
        return p;
    }

    JPanel pageHeader(String title, String subtitle) {
        JPanel h = new JPanel(new GridLayout(0, 1, 0, 2));
        h.setOpaque(false);
        h.add(label(title, font(Font.BOLD, 28), DEEP));
        h.add(label(subtitle, font(Font.PLAIN, 14), MUTED));
        return h;
    }

    JPanel stack(int gap, JComponent... comps) {
        JPanel s = new JPanel(new GridBagLayout());
        s.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        for (int i = 0; i < comps.length; i++) {
            c.gridy = i;
            c.insets = new Insets(0, 0, i < comps.length - 1 ? gap : 0, 0);
            s.add(comps[i], c);
        }
        c.gridy = comps.length;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        s.add(filler, c);
        return s;
    }

    JPanel flow(int align, int gap, JComponent... comps) {
        JPanel p = new JPanel(new FlowLayout(align, gap, 0));
        p.setOpaque(false);
        for (JComponent c : comps) p.add(c);
        return p;
    }

    String htmlHead() {
        return "<html><body style=\"font-family:" + FAMILY + "; color:#1f2937; margin:8px\">";
    }

    // ===================== HOME =====================
    JPanel homePage() {
        JPanel p = page();

        Gradient hero = new Gradient(PRIMARY_DARK, new Color(74, 190, 110), 30, false);
        hero.setLayout(new GridLayout(0, 1, 0, 10));
        hero.setBorder(new EmptyBorder(36, 40, 36, 40));

        JLabel h1 = label("Eat smart. Cook more. Save more.", font(Font.BOLD, 34), Color.WHITE);
        JLabel sub = label("Discover recipes, plan your day and compare home cooking with online prices.",
                           font(Font.PLAIN, 15), new Color(220, 252, 231));

        RoundedButton planBtn = new RoundedButton("Plan my day  \u2192", ACCENT);
        planBtn.addActionListener(e -> { showPage("planner"); generatePlan(); });
        RoundedButton browseBtn = new RoundedButton("Browse recipes", GHOST);
        browseBtn.addActionListener(e -> showPage("discover"));

        hero.add(h1);
        hero.add(sub);
        hero.add(flow(FlowLayout.LEFT, 12, planBtn, browseBtn));

        int totalSaving = 0;
        for (Food f : foods) totalSaving += f.onlinePrice - f.homePrice;
        int avgSaving = foods.isEmpty() ? 0 : totalSaving / foods.size();

        JPanel stats = new JPanel(new GridLayout(1, 4, 18, 0));
        stats.setOpaque(false);
        stats.add(statCard(String.valueOf(foods.size()), "RECIPES", PRIMARY));
        stats.add(statCard("4", "MEAL CATEGORIES", ACCENT));
        stats.add(statCard(RS + avgSaving, "AVG SAVING / MEAL", PRIMARY_DARK));
        stats.add(statCard("6", "SMART TOOLS", new Color(37, 99, 235)));

        Food pick = foods.get(new Random().nextInt(foods.size()));
        Card pickCard = new Card(new BorderLayout(20, 0));
        JPanel pickText = new JPanel(new GridLayout(0, 1, 0, 4));
        pickText.setOpaque(false);
        pickText.add(label("TODAY'S PICK", font(Font.BOLD, 12), ACCENT));
        pickText.add(label(pick.name, font(Font.BOLD, 24), DEEP));
        pickText.add(label(pick.category + "  \u2022  " + pick.cuisine + "  \u2022  Home " + RS + pick.homePrice
                           + " vs Online " + RS + pick.onlinePrice, font(Font.PLAIN, 14), MUTED));
        JLabel pickIcon = new JLabel(emoji(pick.category));
        pickIcon.setFont(emojiFont(44));
        RoundedButton viewPick = new RoundedButton("View recipe", PRIMARY);
        viewPick.addActionListener(e -> openRecipe(pick));
        pickCard.add(pickIcon, BorderLayout.WEST);
        pickCard.add(pickText, BorderLayout.CENTER);
        pickCard.add(viewPick, BorderLayout.EAST);

        p.add(stack(22, hero, stats, pickCard), BorderLayout.CENTER);
        return p;
    }

    Card statCard(String value, String name, Color color) {
        Card c = new Card(new GridLayout(2, 1, 0, 4));
        JLabel v = label(value, font(Font.BOLD, 30), color);
        JLabel n = label(name, font(Font.BOLD, 11), MUTED);
        v.setHorizontalAlignment(SwingConstants.CENTER);
        n.setHorizontalAlignment(SwingConstants.CENTER);
        c.add(v);
        c.add(n);
        return c;
    }

    void openRecipe(Food f) {
        searchField.setText("");
        categoryBox.setSelectedIndex(0);
        showPage("discover");
        foodList.setSelectedValue(f.name, true);
    }

    // ===================== DISCOVER =====================
    class FoodCell extends JPanel implements ListCellRenderer<String> {
        JLabel icon = new JLabel("", SwingConstants.CENTER);
        JLabel name = new JLabel(), meta = new JLabel(), price = new JLabel();
        boolean selected;

        FoodCell() {
            super(new BorderLayout(12, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(8, 14, 8, 14));
            icon.setFont(emojiFont(22));
            icon.setPreferredSize(new Dimension(36, 36));
            name.setFont(font(Font.BOLD, 15));
            meta.setFont(font(Font.PLAIN, 12));
            meta.setForeground(MUTED);
            price.setFont(font(Font.BOLD, 14));
            price.setForeground(PRIMARY_DARK);
            JPanel text = new JPanel(new GridLayout(2, 1));
            text.setOpaque(false);
            text.add(name);
            text.add(meta);
            add(icon, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
            add(price, BorderLayout.EAST);
        }

        @Override public Component getListCellRendererComponent(
                JList<? extends String> list, String value, int index,
                boolean isSelected, boolean cellHasFocus) {
            Food f = findFood(value);
            if (f != null) {
                icon.setText(emoji(f.category));
                name.setText(f.name);
                meta.setText(f.category + "  \u2022  " + f.cuisine);
                price.setText(RS + f.homePrice);
            }
            selected = isSelected;
            name.setForeground(isSelected ? DEEP : TEXT);
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            if (selected) {
                Graphics2D g2 = (Graphics2D) g.create();
                smooth(g2);
                g2.setColor(SOFT);
                g2.fillRoundRect(4, 2, getWidth() - 8, getHeight() - 4, 18, 18);
                g2.setColor(PRIMARY);
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRoundRect(4, 2, getWidth() - 9, getHeight() - 5, 18, 18);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    JPanel discoverPage() {
        JPanel p = page();

        styleCombo(categoryBox);
        categoryBox.setPreferredSize(new Dimension(150, 42));
        searchField.setPreferredSize(new Dimension(300, 42));

        RoundedButton reset = new RoundedButton("Reset", new Color(107, 114, 128)).small();
        reset.addActionListener(e -> {
            searchField.setText("");
            categoryBox.setSelectedIndex(0);
        });

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);
        bar.add(searchField);
        bar.add(categoryBox);
        bar.add(reset);

        JPanel north = new JPanel(new BorderLayout(0, 14));
        north.setOpaque(false);
        north.add(pageHeader("Discover Delicious Food", "Search by name or filter by meal type."),
                  BorderLayout.NORTH);
        north.add(bar, BorderLayout.CENTER);
        p.add(north, BorderLayout.NORTH);

        // list card
        foodList.setCellRenderer(new FoodCell());
        foodList.setFixedCellHeight(62);
        foodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        foodList.setBackground(Color.WHITE);
        JScrollPane listScroll = new JScrollPane(foodList);
        styleScroll(listScroll);

        Card listCard = new Card(new BorderLayout());
        listCard.setBorder(new EmptyBorder(12, 12, 18, 12));
        listCard.setPreferredSize(new Dimension(370, 0));
        listCard.add(listScroll, BorderLayout.CENTER);

        // detail card
        details.setEditable(false);
        details.setBackground(Color.WHITE);
        details.setBorder(new EmptyBorder(6, 10, 6, 10));
        JScrollPane detailScroll = new JScrollPane(details);
        styleScroll(detailScroll);

        Card detailCard = new Card(new BorderLayout());
        detailCard.setBorder(new EmptyBorder(12, 12, 18, 12));
        detailCard.add(detailScroll, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.setOpaque(false);
        center.add(listCard, BorderLayout.WEST);
        center.add(detailCard, BorderLayout.CENTER);
        p.add(center, BorderLayout.CENTER);

        // actions
        RoundedButton addBtn = new RoundedButton("\uFF0B  Add ingredients to cart", PRIMARY);
        RoundedButton randomBtn = new RoundedButton("Surprise me", ACCENT);
        addBtn.addActionListener(e -> {
            Food f = selectedFood();
            if (f != null) addToCart(f);
            else setStatus("Please select a food first.");
        });
        randomBtn.addActionListener(e -> {
            if (foodModel.isEmpty()) return;
            foodList.setSelectedIndex(new Random().nextInt(foodModel.size()));
            foodList.ensureIndexIsVisible(foodList.getSelectedIndex());
        });
        p.add(flow(FlowLayout.RIGHT, 12, randomBtn, addBtn), BorderLayout.SOUTH);

        // listeners
        foodList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedRecipe();
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { filterFoods(); }
            public void removeUpdate(DocumentEvent e)  { filterFoods(); }
            public void changedUpdate(DocumentEvent e) { filterFoods(); }
        });
        categoryBox.addActionListener(e -> filterFoods());

        refreshFoodList(foods);
        return p;
    }

    void refreshFoodList(List<Food> list) {
        foodModel.clear();
        for (Food f : list) foodModel.addElement(f.name);
        if (!list.isEmpty()) foodList.setSelectedIndex(0);
        else showSelectedRecipe();
    }

    void filterFoods() {
        String keyword = searchField.getText().trim().toLowerCase();
        String category = categoryBox.getSelectedItem().toString();
        List<Food> result = new ArrayList<>();
        for (Food f : foods) {
            boolean nameMatch = f.name.toLowerCase().contains(keyword);
            boolean catMatch = category.equals("All") || f.category.equalsIgnoreCase(category);
            if (nameMatch && catMatch) result.add(f);
        }
        refreshFoodList(result);
        setStatus(result.size() + " matching recipes found.");
    }

    Food selectedFood() {
        String name = foodList.getSelectedValue();
        return name == null ? null : findFood(name);
    }

    void showSelectedRecipe() {
        Food f = selectedFood();
        if (f == null) {
            details.setText(htmlHead()
                + "<p style='color:#6b7280'>Select a recipe from the list to see its details.</p></body></html>");
            return;
        }
        int saving = f.onlinePrice - f.homePrice;
        StringBuilder sb = new StringBuilder(htmlHead());
        sb.append("<h1 style='color:#14532d; margin-bottom:0'>").append(f.name).append("</h1>");
        sb.append("<p style='color:#6b7280'>").append(f.category).append(" &bull; ")
          .append(f.cuisine).append("</p>");
        sb.append("<h3 style='color:#16a34a'>Ingredients</h3><ul>");
        for (String ing : f.ingredients) sb.append("<li>").append(ing).append("</li>");
        sb.append("</ul><h3 style='color:#16a34a'>Cooking steps</h3><ol>");
        for (String s : f.steps) sb.append("<li>").append(s).append("</li>");
        sb.append("</ol><table cellpadding='10' cellspacing='8'><tr>");
        sb.append("<td bgcolor='#dcfce7'><font color='#166534'>Home cost</font><br><b>")
          .append(RS).append(f.homePrice).append("</b></td>");
        sb.append("<td bgcolor='#ffedd5'><font color='#9a3412'>Online cost</font><br><b>")
          .append(RS).append(f.onlinePrice).append("</b></td>");
        sb.append("<td bgcolor='#dbeafe'><font color='#1e40af'>You save</font><br><b>")
          .append(RS).append(saving).append("</b></td>");
        sb.append("</tr></table></body></html>");
        details.setText(sb.toString());
        details.setCaretPosition(0);
    }

    // ===================== MEAL PLANNER =====================
    JPanel plannerPage() {
        JPanel p = page();
        p.add(pageHeader("Your Daily Meal Planner", "One click to plan breakfast, lunch, snack and dinner."),
              BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(1, 4, 18, 0));
        grid.setOpaque(false);
        for (int i = 0; i < 4; i++) {
            Card c = new Card(new GridLayout(0, 1, 0, 6));
            c.setPreferredSize(new Dimension(200, 220));
            JLabel ic = new JLabel(emoji(MEALS[i]), SwingConstants.CENTER);
            ic.setFont(emojiFont(40));
            JLabel meal = label(MEALS[i].toUpperCase(), font(Font.BOLD, 12), ACCENT);
            meal.setHorizontalAlignment(SwingConstants.CENTER);
            planName[i] = label("-", font(Font.BOLD, 17), DEEP);
            planName[i].setHorizontalAlignment(SwingConstants.CENTER);
            planCost[i] = label(" ", font(Font.PLAIN, 13), MUTED);
            planCost[i].setHorizontalAlignment(SwingConstants.CENTER);
            c.add(ic);
            c.add(meal);
            c.add(planName[i]);
            c.add(planCost[i]);
            grid.add(c);
        }

        Card summary = new Card(new BorderLayout());
        planSummary.setFont(font(Font.PLAIN, 15));
        planSummary.setForeground(TEXT);
        planSummary.setText("Press \"Generate Meal Plan\" to build today's plan.");
        summary.add(planSummary, BorderLayout.CENTER);

        RoundedButton gen = new RoundedButton("\u2728  Generate Meal Plan", PRIMARY);
        RoundedButton addAll = new RoundedButton("Add whole plan to cart", ACCENT);
        gen.addActionListener(e -> generatePlan());
        addAll.addActionListener(e -> {
            boolean any = false;
            for (Food f : plan) {
                if (f != null) { cart.merge(f.name, 1, Integer::sum); any = true; }
            }
            if (any) { refreshCart(); setStatus("Meal plan ingredients added to cart!"); }
            else setStatus("Generate a meal plan first.");
        });

        p.add(stack(22, grid, summary, flow(FlowLayout.LEFT, 12, gen, addAll)), BorderLayout.CENTER);
        return p;
    }

    void generatePlan() {
        Random random = new Random();
        int home = 0, online = 0;
        for (int i = 0; i < MEALS.length; i++) {
            List<Food> options = new ArrayList<>();
            for (Food f : foods) if (f.category.equals(MEALS[i])) options.add(f);
            if (options.isEmpty()) continue;
            Food sel = options.get(random.nextInt(options.size()));
            plan[i] = sel;
            planName[i].setText("<html><div style='text-align:center;width:140px'>" + sel.name + "</div></html>");
            planCost[i].setText("Home " + RS + sel.homePrice + "  |  Online " + RS + sel.onlinePrice);
            home += sel.homePrice;
            online += sel.onlinePrice;
        }
        planSummary.setText("<html><b style='color:#14532d'>Today's total:</b> cook at home for <b>"
            + RS + home + "</b> instead of <b>" + RS + online + "</b> online &mdash; you save <b style='color:#16a34a'>"
            + RS + (online - home) + "</b>.</html>");
        setStatus("Meal plan generated!");
    }

    // ===================== NEW FOOD MAKER =====================
    JPanel makerPage() {
        JPanel p = page();

        ingredientField.setPreferredSize(new Dimension(420, 42));
        RoundedButton find = new RoundedButton("Find Recipes", PRIMARY);
        find.addActionListener(e -> findRecipes());
        ingredientField.addActionListener(e -> findRecipes());

        JPanel row = flow(FlowLayout.LEFT, 12, ingredientField, find);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        chips.add(label("Try:", font(Font.PLAIN, 13), MUTED));
        for (String s : new String[]{"tomato, onion", "rice, dal", "paneer, onion", "banana, milk"}) {
            RoundedButton chip = new RoundedButton(s, SOFT).small().text(DEEP);
            chip.addActionListener(e -> { ingredientField.setText(s); findRecipes(); });
            chips.add(chip);
        }

        JPanel north = new JPanel(new BorderLayout(0, 12));
        north.setOpaque(false);
        north.add(pageHeader("New Food Maker", "Type the ingredients you have, separated by commas."),
                  BorderLayout.NORTH);
        north.add(stack(10, row, chips), BorderLayout.CENTER);
        north.setPreferredSize(new Dimension(0, 190));
        p.add(north, BorderLayout.NORTH);

        makerOutput.setEditable(false);
        makerOutput.setBackground(Color.WHITE);
        makerOutput.setBorder(new EmptyBorder(6, 10, 6, 10));
        makerOutput.setText(htmlHead()
            + "<p style='color:#6b7280'>Your recipe matches will appear here.</p></body></html>");
        JScrollPane sp = new JScrollPane(makerOutput);
        styleScroll(sp);
        Card card = new Card(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 12, 18, 12));
        card.add(sp, BorderLayout.CENTER);
        p.add(card, BorderLayout.CENTER);
        return p;
    }

    void findRecipes() {
        String input = ingredientField.getText().trim().toLowerCase();
        if (input.isEmpty()) {
            setStatus("Please enter your ingredients.");
            return;
        }
        List<String> available = new ArrayList<>();
        for (String s : input.split(",")) if (!s.trim().isEmpty()) available.add(s.trim());

        class Match {
            Food food; List<String> have = new ArrayList<>(), missing = new ArrayList<>();
        }
        List<Match> matches = new ArrayList<>();
        for (Food f : foods) {
            Match m = new Match();
            m.food = f;
            for (String ing : f.ingredients) {
                boolean hit = false;
                for (String item : available) {
                    if (ing.toLowerCase().contains(item) || item.contains(ing.toLowerCase())) { hit = true; break; }
                }
                (hit ? m.have : m.missing).add(ing);
            }
            if (!m.have.isEmpty()) matches.add(m);
        }
        matches.sort((a, b) -> Integer.compare(b.have.size(), a.have.size()));

        StringBuilder sb = new StringBuilder(htmlHead());
        if (matches.isEmpty()) {
            sb.append("<p style='color:#6b7280'>No matching recipes found. Try different ingredients.</p>");
        } else {
            sb.append("<h2 style='color:#14532d'>").append(matches.size()).append(" recipe(s) you can make</h2>");
            for (Match m : matches) {
                int total = m.food.ingredients.length;
                int pct = m.have.size() * 100 / total;
                sb.append("<table width='100%' cellpadding='10' cellspacing='0'><tr><td bgcolor='#f0fdf4'>");
                sb.append("<b style='font-size:15px;color:#14532d'>").append(m.food.name).append("</b> ")
                  .append("<font color='#6b7280'>&nbsp;").append(m.food.category).append("</font><br>");
                sb.append("Matched <b>").append(m.have.size()).append("/").append(total).append("</b> (")
                  .append(pct).append("%): ").append(String.join(", ", m.have)).append("<br>");
                if (!m.missing.isEmpty())
                    sb.append("<font color='#c2410c'>Still need: ").append(String.join(", ", m.missing)).append("</font><br>");
                sb.append("<table width='100%' cellpadding='3' cellspacing='0'><tr>");
                sb.append("<td width='").append(pct).append("%' bgcolor='#16a34a'></td>");
                if (pct < 100) sb.append("<td width='").append(100 - pct).append("%' bgcolor='#d1d5db'></td>");
                sb.append("</tr></table>");
                sb.append("</td></tr></table><br>");
            }
        }
        sb.append("</body></html>");
        makerOutput.setText(sb.toString());
        makerOutput.setCaretPosition(0);
        setStatus(matches.size() + " recipe match(es) found.");
    }

    // ===================== PRICE COMPARE =====================
    class PriceChart extends Card {
        Food food;
        float progress = 0f;
        Timer t;

        PriceChart() {
            super(new BorderLayout());
        }

        void show(Food f) {
            food = f;
            progress = 0f;
            if (t != null) t.stop();
            t = new Timer(14, e -> {
                progress += 0.05f;
                if (progress >= 1f) { progress = 1f; ((Timer) e.getSource()).stop(); }
                repaint();
            });
            t.start();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth(), h = getHeight();

            if (food == null) {
                g2.setColor(MUTED);
                g2.setFont(font(Font.PLAIN, 16));
                String s = "Choose a food to compare prices";
                g2.drawString(s, (w - g2.getFontMetrics().stringWidth(s)) / 2, h / 2);
                g2.dispose();
                return;
            }

            float ease = 1f - (float) Math.pow(1f - progress, 3);
            int x0 = 44, maxW = w - 2 * x0 - 110;
            int max = Math.max(food.homePrice, food.onlinePrice);

            g2.setColor(DEEP);
            g2.setFont(font(Font.BOLD, 24));
            g2.drawString(food.name, x0, 56);
            g2.setColor(MUTED);
            g2.setFont(font(Font.PLAIN, 13));
            g2.drawString(food.category + "  \u2022  " + food.cuisine, x0, 78);

            drawBar(g2, x0, 118, maxW, "Home cooking", food.homePrice, max, ease, PRIMARY);
            drawBar(g2, x0, 204, maxW, "Online ordering", food.onlinePrice, max, ease, ACCENT);

            int saving = food.onlinePrice - food.homePrice;
            int pct = food.onlinePrice == 0 ? 0 : Math.abs(saving) * 100 / food.onlinePrice;
            String msg = saving >= 0
                ? "You save " + RS + saving + " (" + pct + "%) by cooking at home!"
                : "Ordering online is " + RS + (-saving) + " cheaper for this meal.";
            g2.setFont(font(Font.BOLD, 15));
            FontMetrics fm = g2.getFontMetrics();
            int pw = fm.stringWidth(msg) + 40, ph = 42, py = 290;
            g2.setColor(saving >= 0 ? SOFT : new Color(255, 237, 213));
            g2.fillRoundRect(x0, py, pw, ph, 24, 24);
            g2.setColor(saving >= 0 ? DEEP : new Color(154, 52, 18));
            g2.drawString(msg, x0 + 20, py + (ph - fm.getHeight()) / 2 + fm.getAscent());

            g2.setColor(MUTED);
            g2.setFont(font(Font.ITALIC, 12));
            g2.drawString("Prices are illustrative estimates.", x0, py + ph + 28);
            g2.dispose();
        }

        private void drawBar(Graphics2D g2, int x, int y, int maxW, String name,
                             int value, int max, float ease, Color color) {
            g2.setColor(TEXT);
            g2.setFont(font(Font.BOLD, 14));
            g2.drawString(name, x, y);
            g2.setColor(new Color(236, 240, 236));
            g2.fillRoundRect(x, y + 10, maxW, 34, 18, 18);
            int bw = Math.max(20, (int) (maxW * ((double) value / max) * ease));
            g2.setPaint(new GradientPaint(x, 0, mix(color, Color.WHITE, 0.25), x + bw, 0, color));
            g2.fillRoundRect(x, y + 10, bw, 34, 18, 18);
            g2.setPaint(null);
            g2.setColor(color.darker());
            g2.setFont(font(Font.BOLD, 16));
            g2.drawString(RS + (int) (value * ease), x + maxW + 14, y + 33);
        }
    }

    JPanel comparePage() {
        JPanel p = page();

        JComboBox<String> choices = new JComboBox<>();
        for (Food f : foods) choices.addItem(f.name);
        styleCombo(choices);
        choices.setPreferredSize(new Dimension(230, 42));

        chart = new PriceChart();
        RoundedButton compare = new RoundedButton("Compare Prices", PRIMARY);
        compare.addActionListener(e -> {
            Food f = findFood(String.valueOf(choices.getSelectedItem()));
            if (f != null) { chart.show(f); setStatus("Compared prices for " + f.name + "."); }
        });
        choices.addActionListener(e -> compare.doClick());

        JPanel north = new JPanel(new BorderLayout(0, 14));
        north.setOpaque(false);
        north.add(pageHeader("Home Cooking vs Online Ordering", "See how much you save by cooking at home."),
                  BorderLayout.NORTH);
        north.add(flow(FlowLayout.LEFT, 12, label("Choose food:", font(Font.BOLD, 14), TEXT), choices, compare),
                  BorderLayout.CENTER);
        p.add(north, BorderLayout.NORTH);
        p.add(chart, BorderLayout.CENTER);

        chart.show(foods.get(0));
        return p;
    }

    // ===================== GROCERY CART =====================
    JPanel cartPage() {
        JPanel p = page();
        p.add(pageHeader("Your Grocery Cart", "Adjust quantities, remove items or check out."),
              BorderLayout.NORTH);

        cartRows.setLayout(new BoxLayout(cartRows, BoxLayout.Y_AXIS));
        cartRows.setBackground(Color.WHITE);
        JScrollPane sp = new JScrollPane(cartRows);
        styleScroll(sp);

        Card listCard = new Card(new BorderLayout());
        listCard.setBorder(new EmptyBorder(12, 16, 18, 16));
        listCard.add(sp, BorderLayout.CENTER);
        p.add(listCard, BorderLayout.CENTER);

        cartTotal.setFont(font(Font.BOLD, 22));
        cartTotal.setForeground(DEEP);

        RoundedButton clear = new RoundedButton("Clear cart", DANGER);
        RoundedButton checkout = new RoundedButton("Checkout  \u2192", PRIMARY);
        clear.addActionListener(e -> {
            cart.clear();
            refreshCart();
            setStatus("Cart cleared.");
        });
        checkout.addActionListener(e -> {
            if (cart.isEmpty()) { setStatus("Your cart is empty."); return; }
            int total = cartTotalValue();
            JOptionPane.showMessageDialog(this,
                "Your grocery list is ready!\n\nItems: " + cartCount() + "\nTotal: " + RS + total
                + "\n\n(Demo checkout)", "FOODVERSE Checkout", JOptionPane.INFORMATION_MESSAGE);
            cart.clear();
            refreshCart();
        });

        Card bottom = new Card(new BorderLayout());
        bottom.setBorder(new EmptyBorder(8, 22, 14, 22));
        bottom.add(cartTotal, BorderLayout.WEST);
        bottom.add(flow(FlowLayout.RIGHT, 12, clear, checkout), BorderLayout.EAST);
        p.add(bottom, BorderLayout.SOUTH);

        refreshCart();
        return p;
    }

    int cartCount() {
        int n = 0;
        for (int q : cart.values()) n += q;
        return n;
    }

    int cartTotalValue() {
        int total = 0;
        for (Map.Entry<String, Integer> en : cart.entrySet()) {
            Food f = findFood(en.getKey());
            if (f != null) total += f.homePrice * en.getValue();
        }
        return total;
    }

    void refreshCart() {
        cartRows.removeAll();
        if (cart.isEmpty()) {
            JLabel empty = label("Your cart is empty. Add recipes from Discover!", font(Font.PLAIN, 15), MUTED);
            empty.setBorder(new EmptyBorder(30, 10, 0, 0));
            cartRows.add(empty);
        } else {
            for (Map.Entry<String, Integer> en : new ArrayList<>(cart.entrySet())) {
                Food f = findFood(en.getKey());
                if (f != null) cartRows.add(cartRow(f, en.getValue()));
            }
        }
        cartTotal.setText("Total:  " + RS + cartTotalValue());
        NavButton nb = navs.get("cart");
        if (nb != null) nb.setBadge(cartCount());
        cartRows.revalidate();
        cartRows.repaint();
    }

    JPanel cartRow(Food f, int qty) {
        JPanel r = new JPanel(new BorderLayout(14, 0));
        r.setBackground(Color.WHITE);
        r.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(12, 6, 12, 6)));
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        JLabel icon = new JLabel(emoji(f.category));
        icon.setFont(emojiFont(24));
        JLabel name = new JLabel("<html><b style='font-size:13px'>" + f.name
            + "</b><br><font color='#6b7280'>" + RS + f.homePrice + " each</font></html>");
        name.setFont(font(Font.PLAIN, 13));
        JPanel left = new JPanel(new BorderLayout(12, 0));
        left.setOpaque(false);
        left.add(icon, BorderLayout.WEST);
        left.add(name, BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(260, 40));

        RoundedButton minus = new RoundedButton("\u2212", new Color(107, 114, 128)).small();
        RoundedButton plus = new RoundedButton("+", PRIMARY).small();
        JLabel q = label(String.valueOf(qty), font(Font.BOLD, 16), DEEP);
        q.setHorizontalAlignment(SwingConstants.CENTER);
        q.setPreferredSize(new Dimension(34, 24));
        minus.addActionListener(e -> {
            int n = cart.getOrDefault(f.name, 1) - 1;
            if (n <= 0) cart.remove(f.name); else cart.put(f.name, n);
            refreshCart();
        });
        plus.addActionListener(e -> {
            cart.merge(f.name, 1, Integer::sum);
            refreshCart();
        });
        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        qtyPanel.setOpaque(false);
        qtyPanel.add(minus);
        qtyPanel.add(q);
        qtyPanel.add(plus);

        JLabel cost = label(RS + (f.homePrice * qty), font(Font.BOLD, 17), PRIMARY_DARK);
        RoundedButton remove = new RoundedButton("\u2715", DANGER).small();
        remove.addActionListener(e -> {
            cart.remove(f.name);
            refreshCart();
            setStatus(f.name + " removed.");
        });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 6));
        right.setOpaque(false);
        right.add(cost);
        right.add(remove);

        r.add(left, BorderLayout.WEST);
        r.add(qtyPanel, BorderLayout.CENTER);
        r.add(right, BorderLayout.EAST);
        return r;
    }

    void addToCart(Food f) {
        cart.merge(f.name, 1, Integer::sum);
        refreshCart();
        setStatus(f.name + " added to grocery cart!");
    }

    Food findFood(String name) {
        for (Food f : foods) if (f.name.equalsIgnoreCase(name)) return f;
        return null;
    }

    // ===================== PROGRAM START =====================
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.out.println("Using default Java appearance.");
            }
            UIManager.put("OptionPane.messageFont", font(Font.PLAIN, 14));
            UIManager.put("OptionPane.buttonFont", font(Font.BOLD, 13));
            new FoodVerse().setVisible(true);
        });
    }
}