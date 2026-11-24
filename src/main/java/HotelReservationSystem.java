import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Vector;
import haven.theme.HavenColors;
import haven.theme.ModernButton;

/**
 * Hotel Reservation System
 * Single-class Java Swing application backed by SQLite.
 * Java 17+; SQLite database is created automatically on first launch.
 */
public class HotelReservationSystem extends JFrame {
    private static final String DB_URL = "jdbc:sqlite:hotel_reservations.db";
    private static final Color NAVY = ThemePalette.MIDNIGHT_SLATE;
    private static final Color BLUE = ThemePalette.DEEP_TEAL;
    private static final Color PALE = ThemePalette.SOFT_ALABASTER;
    private static final Color GREEN = ThemePalette.DEEP_TEAL;

    private final JTextField guestField = new JTextField(18);
    private final JTextField phoneField = new JTextField(18);
    private final JTextField emailField = new JTextField(18);
    private final JComboBox<String> roomCombo = new JComboBox<>();
    private final JTextField checkInField = new JTextField("2026-12-01", 18);
    private final JTextField checkOutField = new JTextField("2026-12-03", 18);
    private final JSpinner guestsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
    private final JTextField searchField = new JTextField(20);
    private final JLabel statusLabel = new JLabel("Ready");
    private final JLabel roomsMetric = new JLabel("0");
    private final JLabel bookingsMetric = new JLabel("0");
    private final JLabel guestsMetric = new JLabel("0");
    private final JLabel occupiedMetric = new JLabel("0");
    private DefaultTableModel reservationModel;
    private JTable reservationTable;
    private JTable roomTable;
    private DefaultTableModel dashboardRecentModel;
    private DefaultTableModel reservationRoomModel;
    private JTable reservationRoomTable;
    private JLabel reservationGrandTotal;
    private JTextField reservationName, reservationPhone, reservationEmail, reservationIdNumber, reservationSecondaryPhone;
    private JCheckBox immediateCheckIn;

    public HotelReservationSystem() {
        super("Haven Hotel | Reservation Management");
        initializeDatabase();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 760));
        setSize(1500, 920);
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemePalette.SOFT_ALABASTER);
        setContentPane(root);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildMainContent(), BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);
        refreshAll();
    }

    private JTabbedPane findTabbedPane(Container parent) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JTabbedPane tabs) return tabs;
            if (component instanceof Container child) {
                JTabbedPane found = findTabbedPane(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void initializeDatabase() {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS rooms (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    room_number TEXT NOT NULL UNIQUE,
                    room_type TEXT NOT NULL,
                    price REAL NOT NULL,
                    capacity INTEGER NOT NULL
                )
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS guests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    email TEXT
                )
            """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    guest_id INTEGER NOT NULL,
                    room_id INTEGER NOT NULL,
                    check_in TEXT NOT NULL,
                    check_out TEXT NOT NULL,
                    guests INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'CONFIRMED',
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (guest_id) REFERENCES guests(id),
                    FOREIGN KEY (room_id) REFERENCES rooms(id)
                )
            """);
            ensureColumn(c, "guests", "secondary_phone", "TEXT");
            ensureColumn(c, "guests", "id_number", "TEXT");
            ensureColumn(c, "reservations", "immediate_checkin", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(c, "reservations", "days", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(c, "reservations", "nightly_rate", "REAL NOT NULL DEFAULT 0");
            ensureColumn(c, "reservations", "extra_bed", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(c, "reservations", "extra_bed_rate", "REAL NOT NULL DEFAULT 0");
            ensureColumn(c, "reservations", "total_amount", "REAL NOT NULL DEFAULT 0");
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM rooms")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    st.executeUpdate("""
                        INSERT INTO rooms(room_number, room_type, price, capacity) VALUES
                        ('101','Standard',55,2),('102','Standard',55,2),
                        ('201','Deluxe',85,3),('202','Deluxe',85,3),
                        ('301','Family Suite',130,5),('302','Executive Suite',175,4)
                    """);
                }
            }
        } catch (SQLException ex) {
            showError("Could not initialize the SQLite database.\n" + ex.getMessage());
        }
    }

    private void ensureColumn(Connection c,String table,String column,String definition)throws SQLException{
        boolean found=false;
        try(Statement q=c.createStatement();ResultSet rs=q.executeQuery("PRAGMA table_info("+table+")")){
            while(rs.next())if(column.equalsIgnoreCase(rs.getString("name"))){found=true;break;}
        }
        if(!found)try(Statement q=c.createStatement()){q.executeUpdate("ALTER TABLE "+table+" ADD COLUMN "+column+" "+definition);}
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(22, 0));
        header.setBackground(HavenColors.MIDNIGHT_SLATE);
        header.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, HavenColors.CHAMPAGNE_GOLD),
                new EmptyBorder(10, 28, 10, 28)));
        header.setPreferredSize(new Dimension(0, 126));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        SkylineMark skyline = new SkylineMark();
        skyline.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel brandName = new JLabel("HAVEN / STAYS");
        brandName.setAlignmentX(Component.CENTER_ALIGNMENT);
        brandName.setForeground(HavenColors.CHAMPAGNE_GOLD);
        brandName.setFont(new Font("Serif", Font.BOLD, 25));
        JLabel tagline = new JLabel("Luxury  •  Comfort  •  Elegance");
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        tagline.setForeground(new Color(225, 214, 190));
        tagline.setFont(new Font("Serif", Font.ITALIC, 11));
        brand.add(skyline); brand.add(brandName); brand.add(tagline);
        brand.setPreferredSize(new Dimension(270, 100));

        JPanel heading = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 23));
        heading.setOpaque(false);
        JLabel bed = new JLabel("▰");
        bed.setForeground(HavenColors.CHAMPAGNE_GOLD);
        bed.setFont(new Font("SansSerif", Font.BOLD, 36));
        JLabel pageTitle = new JLabel("Reservation");
        pageTitle.setForeground(Color.WHITE);
        pageTitle.setFont(new Font("Serif", Font.BOLD, 34));
        heading.add(bed); heading.add(pageTitle);

        JButton admin = new JButton("◉  Admin   ⌄");
        admin.setForeground(Color.WHITE);
        admin.setFont(new Font("SansSerif", Font.BOLD, 14));
        admin.setBorder(new EmptyBorder(12, 18, 12, 8));
        admin.setContentAreaFilled(false); admin.setFocusPainted(false);
        admin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        admin.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Signed in as Administrator", "HAVEN / STAYS Admin", JOptionPane.INFORMATION_MESSAGE));
        header.add(brand, BorderLayout.WEST);
        header.add(heading, BorderLayout.CENTER);
        header.add(admin, BorderLayout.EAST);
        return header;
    }

    private JComponent buildMainContent() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override protected int calculateTabAreaHeight(int placement, int runs, int maxTabHeight) { return 0; }
            @Override protected int calculateTabAreaWidth(int placement, int runs, int maxTabWidth) { return 0; }
        });
        tabs.addTab("HOME", buildHomeScreen());
        tabs.addTab("BOOKING", buildBookingPanel());
        tabs.addTab("RESERVATIONS", buildReservationsPanel());
        tabs.addTab("ROOMS", buildRoomsPanel());
        tabs.setOpaque(false); tabs.setBackground(new Color(0,0,0,0)); tabs.setBorder(null);

        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(new Color(238, 242, 245));
        workspace.add(buildSideNavigation(), BorderLayout.WEST);
        workspace.add(tabs, BorderLayout.CENTER);
        return workspace;
    }

    private JPanel buildHomeScreen() {
        BackgroundImagePanel hero = new BackgroundImagePanel();
        hero.setLayout(new GridBagLayout());
        hero.setBorder(new EmptyBorder(25, 24, 25, 24));

        RoundedPanel card = new RoundedPanel(18, new Color(15, 30, 46, 209));
        card.setLayout(new GridBagLayout());
        card.setBorder(new CompoundBorder(new LineBorder(new Color(200,169,110,210),1),
                new EmptyBorder(27, 34, 25, 34)));
        card.setPreferredSize(new Dimension(690, 300));

        JLabel eyebrow = new JLabel("WELCOME TO HAVEN / STAYS");
        eyebrow.setForeground(HavenColors.CHAMPAGNE_GOLD);
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel headline = new JLabel("Reserve Your Coastal Escape");
        headline.setForeground(HavenColors.CHAMPAGNE_GOLD);
        headline.setFont(new Font("Serif", Font.BOLD, 31));
        JLabel sub = new JLabel("A quieter kind of luxury, beside the sea.");
        sub.setForeground(new Color(226,232,240));
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JTextField in = new JTextField(checkInField.getText(), 13);
        JTextField out = new JTextField(checkOutField.getText(), 13);
        styleHeroField(in); styleHeroField(out);
        JComboBox<String> room = new JComboBox<>();
        for (int i=0;i<roomCombo.getItemCount();i++) room.addItem(roomCombo.getItemAt(i));
        room.setFont(new Font("SansSerif", Font.PLAIN, 12));
        room.setBackground(new Color(244,246,248));
        room.setForeground(new Color(31,41,55));
        JSpinner heroGuests = new JSpinner(new SpinnerNumberModel(1,1,10,1));
        heroGuests.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel form = new JPanel(new GridLayout(2,2,14,8));
        form.setOpaque(false);
        form.add(heroField("CHECK IN  ·  YYYY-MM-DD", in));
        form.add(heroField("CHECK OUT  ·  YYYY-MM-DD", out));
        form.add(heroField("ROOM TYPE / ROOM", room));
        form.add(heroField("GUESTS", heroGuests));

        JButton availability = new JButton("CHECK AVAILABILITY   →");
        availability.setFont(new Font("SansSerif", Font.BOLD, 12));
        availability.setForeground(Color.WHITE);
        availability.setBackground(HavenColors.DEEP_TEAL);
        availability.setBorder(new CompoundBorder(new LineBorder(HavenColors.CHAMPAGNE_GOLD,1,true),
                new EmptyBorder(13,24,13,24)));
        availability.setFocusPainted(false);
        availability.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        availability.addActionListener(e -> {
            checkInField.setText(in.getText().trim());
            checkOutField.setText(out.getText().trim());
            if (room.getSelectedItem()!=null) roomCombo.setSelectedItem(room.getSelectedItem());
            guestsSpinner.setValue(heroGuests.getValue());
            checkHomeAvailability();
        });

        GridBagConstraints g = new GridBagConstraints();
        g.gridx=0; g.gridy=0; g.gridwidth=2; g.anchor=GridBagConstraints.CENTER;
        g.insets=new Insets(0,0,7,0); card.add(eyebrow,g);
        g.gridy++; g.insets=new Insets(0,0,4,0); card.add(headline,g);
        g.gridy++; g.insets=new Insets(0,0,20,0); card.add(sub,g);
        g.gridy++; g.fill=GridBagConstraints.HORIZONTAL; g.weightx=1; g.insets=new Insets(0,0,18,0); card.add(form,g);
        g.gridy++; g.fill=GridBagConstraints.NONE; g.weightx=0; g.insets=new Insets(0,0,0,0); card.add(availability,g);

        hero.add(card);
        return hero;
    }

    private JPanel heroField(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0,6));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setForeground(new Color(226,232,240));
        l.setFont(new Font("SansSerif", Font.BOLD, 9));
        p.add(l,BorderLayout.NORTH);
        p.add(field,BorderLayout.CENTER);
        return p;
    }

    private void styleHeroField(JTextField field) {
        field.setFont(new Font("SansSerif",Font.PLAIN,13));
        field.setForeground(new Color(226,232,240));
        field.setCaretColor(HavenColors.CHAMPAGNE_GOLD);
        field.setBackground(new Color(255,255,255,24));
        field.setBorder(new CompoundBorder(new LineBorder(new Color(226,232,240,100),1,true),
                new EmptyBorder(9,11,9,11)));
    }

    private void checkHomeAvailability() {
        try {
            LocalDate checkIn = LocalDate.parse(checkInField.getText().trim());
            LocalDate checkOut = LocalDate.parse(checkOutField.getText().trim());
            if (!checkOut.isAfter(checkIn)) {
                JOptionPane.showMessageDialog(this,"Check-out must be after check-in.","Invalid dates",JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (roomCombo.getSelectedItem()==null) {
                JOptionPane.showMessageDialog(this,"Please select a room.","Room required",JOptionPane.WARNING_MESSAGE);
                return;
            }
            String selected = roomCombo.getSelectedItem().toString();
            int roomId = Integer.parseInt(selected.split(" - ")[0]);
            String sql = """
                SELECT rm.room_number, rm.room_type FROM rooms rm
                WHERE rm.id=? AND NOT EXISTS (
                  SELECT 1 FROM reservations r
                  WHERE r.room_id=rm.id AND r.status='CONFIRMED'
                    AND r.check_in < ? AND r.check_out > ?
                )
                """;
            try (Connection c=connect(); PreparedStatement p=c.prepareStatement(sql)) {
                p.setInt(1,roomId); p.setString(2,checkOut.toString()); p.setString(3,checkIn.toString());
                try (ResultSet rs=p.executeQuery()) {
                    if (rs.next()) {
                        statusLabel.setText("Availability found for room " + rs.getString("room_number"));
                        JOptionPane.showMessageDialog(this,"Good news! Room " + rs.getString("room_number") +
                                " (" + rs.getString("room_type") + ") is available for your selected dates.\\nContinue with guest details to complete your reservation.",
                                "Room available",JOptionPane.INFORMATION_MESSAGE);
                        selectPage(1);
                    } else {
                        JOptionPane.showMessageDialog(this,"That room is not available for the selected dates. Please choose another room or dates.",
                                "No availability",JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,"Enter dates using YYYY-MM-DD (for example 2026-12-01).",
                    "Check your dates",JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            showError("Availability search failed: " + ex.getMessage());
        }
    }

    private static class BackgroundImagePanel extends JPanel {
        private final Image image;
        BackgroundImagePanel() {
            setOpaque(true);
            java.net.URL url = HotelReservationSystem.class.getResource("/images/home_hero.jpg");
            image = url == null ? null : new ImageIcon(url).getImage();
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g=(Graphics2D)graphics.create();
            int w=getWidth(), h=getHeight();
            if (image!=null && image.getWidth(this)>0) {
                double scale=Math.max((double)w/image.getWidth(this),(double)h/image.getHeight(this));
                int iw=(int)(image.getWidth(this)*scale), ih=(int)(image.getHeight(this)*scale);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.drawImage(image,(w-iw)/2,(h-ih)/2,iw,ih,this);
            } else {
                g.setPaint(new GradientPaint(0,0,new Color(25,55,69),0,h,new Color(8,20,34)));
                g.fillRect(0,0,w,h);
            }
            g.setPaint(new GradientPaint(0,0,new Color(5,15,27,155),0,h,new Color(5,15,27,115)));
            g.fillRect(0,0,w,h);
            g.setPaint(new RadialGradientPaint(new Point(w/2,h/2),Math.max(w,h)*0.72f,
                    new float[]{0f,0.68f,1f},new Color[]{new Color(0,0,0,0),new Color(0,0,0,25),new Color(0,0,0,145)}));
            g.fillRect(0,0,w,h);
            g.dispose();
        }
    }

    private JPanel buildDashboard() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(ThemePalette.SOFT_ALABASTER);
        panel.setBorder(new EmptyBorder(16, 18, 16, 18));

        DashboardHeaderPanel dashboardHeader = new DashboardHeaderPanel();

        JPanel roomPortfolio = new RoundedPanel(16, Color.WHITE);
        roomPortfolio.setLayout(new BorderLayout(8, 10));
        roomPortfolio.setBorder(new CompoundBorder(new LineBorder(ThemePalette.BORDER),
                new EmptyBorder(13, 14, 13, 14)));
        JLabel portfolioTitle = new JLabel("▣  Room Portfolio");
        portfolioTitle.setFont(new Font("Serif", Font.BOLD, 19));
        portfolioTitle.setForeground(ThemePalette.MIDNIGHT_SLATE);
        JButton viewAll = new JButton("View all rooms  →");
        styleTextButton(viewAll);
        viewAll.addActionListener(e -> selectPage(3));
        JPanel portfolioHead = new JPanel(new BorderLayout());
        portfolioHead.setOpaque(false);
        portfolioHead.add(portfolioTitle, BorderLayout.WEST);
        portfolioHead.add(viewAll, BorderLayout.EAST);
        JPanel roomCards = new JPanel(new GridLayout(1, 3, 12, 0));
        roomCards.setOpaque(false);
        roomCards.add(dashboardRoomCard("Standard Room", "$55", "standard-room.png", "Wi-Fi   •   AC   •   Ensuite", "101"));
        roomCards.add(dashboardRoomCard("Deluxe Room", "$85", "deluxe-room.png", "Wi-Fi   •   AC   •   Ensuite", "201"));
        roomCards.add(dashboardRoomCard("Executive Suite", "$175", "suite-room.png", "Wi-Fi   •   AC   •   Breakfast", "302"));
        roomPortfolio.add(portfolioHead, BorderLayout.NORTH);
        roomPortfolio.add(roomCards, BorderLayout.CENTER);

        JPanel quick = new RoundedPanel(16, Color.WHITE);
        quick.setLayout(new BoxLayout(quick, BoxLayout.Y_AXIS));
        quick.setBorder(new CompoundBorder(new LineBorder(ThemePalette.BORDER),
                new EmptyBorder(15, 14, 15, 14)));
        JLabel quickTitle = new JLabel("⚡  Quick Actions");
        quickTitle.setFont(new Font("Serif", Font.BOLD, 19));
        quickTitle.setForeground(ThemePalette.MIDNIGHT_SLATE);
        quick.add(quickTitle);
        quick.add(Box.createVerticalStrut(15));
        quick.add(quickAction("Create new booking", 1));
        quick.add(Box.createVerticalStrut(9));
        quick.add(quickAction("Manage reservations", 2));
        quick.add(Box.createVerticalStrut(9));
        quick.add(quickAction("View rooms", 3));
        quick.add(Box.createVerticalGlue());
        haven.theme.RoundedPanel hospitality = new haven.theme.RoundedPanel(13, new Color(246, 241, 230));
        hospitality.setShadowEnabled(false);
        hospitality.setLayout(new BorderLayout(8, 4));
        hospitality.setBorder(new EmptyBorder(12, 10, 12, 10));
        JLabel bell = new JLabel("♧");
        bell.setForeground(new Color(145, 108, 48));
        bell.setFont(new Font("Serif", Font.BOLD, 25));
        JLabel sideNote = new JLabel("<html><b>Experience True Hospitality</b><br><span style='color:#64748B'>Every stay, thoughtfully managed.</span></html>");
        sideNote.setFont(new Font("SansSerif", Font.PLAIN, 10));
        sideNote.setForeground(ThemePalette.MIDNIGHT_SLATE);
        hospitality.add(bell, BorderLayout.WEST);
        hospitality.add(sideNote, BorderLayout.CENTER);
        hospitality.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        quick.add(hospitality);

        JPanel middle = new JPanel(new BorderLayout(12, 0));
        middle.setOpaque(false);
        middle.add(roomPortfolio, BorderLayout.CENTER);
        quick.setPreferredSize(new Dimension(238, 0));
        middle.add(quick, BorderLayout.EAST);

        JPanel recent = new RoundedPanel(16, Color.WHITE);
        recent.setLayout(new BorderLayout(8, 8));
        recent.setBorder(new CompoundBorder(new LineBorder(ThemePalette.BORDER),
                new EmptyBorder(11, 13, 11, 13)));
        JLabel recentTitle = new JLabel("◷  Recent Reservations");
        recentTitle.setFont(new Font("Serif", Font.BOLD, 18));
        recentTitle.setForeground(ThemePalette.MIDNIGHT_SLATE);
        JButton recentAll = new JButton("View all  →");
        styleTextButton(recentAll);
        recentAll.addActionListener(e -> selectPage(2));
        JPanel recentHead = new JPanel(new BorderLayout());
        recentHead.setOpaque(false);
        recentHead.add(recentTitle, BorderLayout.WEST);
        recentHead.add(recentAll, BorderLayout.EAST);
        dashboardRecentModel = new DefaultTableModel(
                new String[]{"GUEST", "ROOM", "CHECK-IN", "CHECK-OUT", "STATUS"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable recentTable = new JTable(dashboardRecentModel);
        styleTable(recentTable);
        recentTable.setRowHeight(34);
        recentTable.setIntercellSpacing(new Dimension(0, 1));
        recentTable.setShowHorizontalLines(true);
        recentTable.setGridColor(new Color(239, 242, 245));
        recentTable.getTableHeader().setReorderingAllowed(false);
        recentTable.setFillsViewportHeight(true);
        // Render the STATUS column as compact rounded pills (Confirmed = #DCFCE7).
        recentTable.getColumnModel().getColumn(4).setCellRenderer(new StatusPillRenderer());
        recentTable.getColumnModel().getColumn(4).setPreferredWidth(104);
        recentTable.getColumnModel().getColumn(4).setMinWidth(92);
        JScrollPane recentScroll = new JScrollPane(recentTable);
        recentScroll.setBorder(null);
        recentScroll.getViewport().setBackground(Color.WHITE);
        recent.add(recentHead, BorderLayout.NORTH);
        recent.add(recentScroll, BorderLayout.CENTER);
        

        JPanel lower = new JPanel(new BorderLayout());
        lower.setOpaque(false);
        lower.add(recent, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);
        center.add(middle, BorderLayout.CENTER);
        center.add(lower, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.add(dashboardHeader, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel dashboardMetric(String title, JLabel value, String symbol, Color accent) {
        JPanel card = new RoundedPanel(16, Color.WHITE);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(new CompoundBorder(new LineBorder(ThemePalette.BORDER),
                new EmptyBorder(12, 14, 12, 14)));
        JLabel icon = new CircleBadge(symbol, accent);
        icon.setPreferredSize(new Dimension(54, 54));
        icon.setToolTipText(title);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(title);
        label.setFont(new Font("SansSerif", Font.BOLD, 10));
        label.setForeground(new Color(84, 101, 119));
        value.setFont(new Font("Serif", Font.BOLD, 27));
        value.setForeground(ThemePalette.MIDNIGHT_SLATE);
        text.add(label);
        text.add(Box.createVerticalStrut(3));
        text.add(value);
        card.add(icon, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JPanel dashboardRoomCard(String title, String price, String image, String amenities, String roomNumber) {
        haven.theme.RoundedPanel card = new haven.theme.RoundedPanel(16, Color.WHITE);
        card.setLayout(new BorderLayout(0, 7));
        card.setBorder(new CompoundBorder(new EmptyBorder(4, 4, 5, 4), new EmptyBorder(5, 5, 5, 5)));
        card.setPreferredSize(new Dimension(210, 260));
        card.setMinimumSize(new Dimension(175, 245));

        JLabel photo = new JLabel();
        photo.setOpaque(true);
        photo.setBackground(new Color(225, 231, 235));
        photo.setPreferredSize(new Dimension(220, 104));
        photo.setHorizontalAlignment(SwingConstants.CENTER);
        photo.setBorder(BorderFactory.createLineBorder(new Color(232, 236, 240)));
        java.net.URL url = getClass().getResource("/images/" + image);
        if (url != null) {
            ImageIcon source = new ImageIcon(url);
            photo.setIcon(new ImageIcon(source.getImage().getScaledInstance(360, 150, Image.SCALE_SMOOTH)));
        } else {
            photo.setText("HAVEN  /  STAYS");
            photo.setForeground(ThemePalette.MIDNIGHT_SLATE);
            photo.setFont(new Font("Serif", Font.BOLD, 16));
        }

        String category = title.startsWith("Standard") ? "STANDARD" : title.startsWith("Deluxe") ? "DELUXE" : "EXECUTIVE";
        JLabel categoryPill = new JLabel("  " + category + "  ");
        categoryPill.setOpaque(true);
        categoryPill.setBackground(new Color(248, 241, 225));
        categoryPill.setForeground(new Color(145, 108, 48));
        categoryPill.setFont(new Font("SansSerif", Font.BOLD, 9));
        categoryPill.setBorder(new EmptyBorder(4, 3, 4, 3));
        categoryPill.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel name = new JLabel(title);
        name.setFont(new Font("Serif", Font.BOLD, 16));
        name.setForeground(ThemePalette.MIDNIGHT_SLATE);
        JLabel rate = new JLabel(price + "  / night");
        rate.setFont(new Font("SansSerif", Font.BOLD, 14));
        rate.setForeground(ThemePalette.DEEP_TEAL);

        JPanel amenityRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        amenityRow.setOpaque(false);
        amenityRow.add(amenityItem("Wi-Fi", "wifi"));
        amenityRow.add(amenityItem("AC", "ac"));
        amenityRow.add(amenityItem(amenities.toLowerCase().contains("breakfast") ? "Breakfast" : "Ensuite",
                amenities.toLowerCase().contains("breakfast") ? "breakfast" : "ensuite"));

        ModernButton book = new ModernButton("Book Now", HavenColors.DEEP_TEAL);
        book.setCornerRadius(12);
        book.setFont(new Font("SansSerif", Font.BOLD, 11));
        book.setBorder(new EmptyBorder(8, 12, 8, 12));
        book.setPreferredSize(new Dimension(0, 34));
        book.addActionListener(e -> {
            for (int i = 0; i < roomCombo.getItemCount(); i++) {
                if (roomCombo.getItemAt(i).contains(" - " + roomNumber + " ")) {
                    roomCombo.setSelectedIndex(i); break;
                }
            }
            selectPage(1);
            statusLabel.setText(title + " selected. Complete the guest details to reserve it.");
        });

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.add(categoryPill);
        details.add(Box.createVerticalStrut(5));
        details.add(name);
        details.add(Box.createVerticalStrut(2));
        details.add(rate);
        details.add(Box.createVerticalStrut(6));
        details.add(amenityRow);
        details.add(Box.createVerticalStrut(8));
        details.add(book);
        card.add(photo, BorderLayout.NORTH);
        card.add(details, BorderLayout.CENTER);
        return card;
    }

    private JComponent amenityItem(String label, String type) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        item.setOpaque(false);
        JLabel icon = new JLabel(new AmenityVectorIcon(type));
        JLabel text = new JLabel(label);
        text.setFont(new Font("SansSerif", Font.PLAIN, 9));
        text.setForeground(new Color(79, 96, 111));
        item.add(icon);
        item.add(text);
        return item;
    }

    private JButton quickAction(String text, int tab) {
        String iconType = text.toLowerCase().contains("booking") ? "booking"
                : text.toLowerCase().contains("reservation") ? "calendar" : "room";
        ModernButton button = new ModernButton(text, new Color(246, 248, 250));
        button.setTextColor(ThemePalette.MIDNIGHT_SLATE);
        button.setCornerRadius(12);
        button.setFont(new Font("SansSerif", Font.BOLD, 11));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIcon(new QuickActionVectorIcon(iconType));
        button.setIconTextGap(12);
        button.setBorder(new EmptyBorder(10, 12, 10, 12));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        button.setPreferredSize(new Dimension(205, 46));
        button.addActionListener(e -> selectPage(tab));
        return button;
    }

    private void styleTextButton(JButton button) {
        button.setForeground(ThemePalette.CHAMPAGNE_GOLD);
        button.setFont(new Font("SansSerif", Font.BOLD, 11));
        button.setBorder(new EmptyBorder(4, 6, 4, 6));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void selectPage(int index) {
        JTabbedPane tabs = findTabbedPane(getContentPane());
        if (tabs != null) tabs.setSelectedIndex(index);
    }

    private JPanel buildSideNavigation() {
        JPanel side = new JPanel();
        side.setBackground(HavenColors.MIDNIGHT_SLATE);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(18, 9, 16, 9));
        side.setPreferredSize(new Dimension(232, 0));

        JLabel monogram = new JLabel("H  S", SwingConstants.CENTER);
        monogram.setForeground(HavenColors.CHAMPAGNE_GOLD);
        monogram.setFont(new Font("Serif", Font.BOLD, 54));
        monogram.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sideBrand = new JLabel("HAVEN / STAYS", SwingConstants.CENTER);
        sideBrand.setForeground(HavenColors.CHAMPAGNE_GOLD);
        sideBrand.setFont(new Font("Serif", Font.BOLD, 17));
        sideBrand.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(monogram); side.add(sideBrand); side.add(Box.createVerticalStrut(28));

        String[] names = {"Dashboard", "Rooms", "Reservations", "Customers", "Payments", "Reports", "Settings", "About"};
        int[] pages = {0, 3, 2, 2, 2, 2, 0, 0};
        String[] iconTypes = {"dashboard", "rooms", "reservations", "customers", "payments", "reports", "settings", "about"};
        for (int i = 0; i < names.length; i++) {
            final int page = pages[i];
            boolean active = i == 2;
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            row.setPreferredSize(new Dimension(190, 46));
            row.setBorder(new EmptyBorder(2, 5, 2, 5));
            JLabel icon = new JLabel(new SidebarVectorIcon(iconTypes[i], active ? HavenColors.MIDNIGHT_SLATE : new Color(190, 204, 216)));
            icon.setPreferredSize(new Dimension(24, 28));
            JLabel text = new JLabel(names[i]);
            text.setFont(new Font("SansSerif", Font.BOLD, 12));
            text.setForeground(active ? HavenColors.MIDNIGHT_SLATE : new Color(224, 232, 238));
            row.add(icon, BorderLayout.WEST);
            row.add(text, BorderLayout.CENTER);
            if (active) {
                row.setBackground(HavenColors.CHAMPAGNE_GOLD);
                row.setOpaque(true);
                row.setBorder(new EmptyBorder(2, 10, 2, 10));
            }
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            row.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { selectPage(page); }
                @Override public void mouseEntered(MouseEvent e) { if (!active) row.setBackground(new Color(29, 49, 66)); if (!active) row.setOpaque(true); row.repaint(); }
                @Override public void mouseExited(MouseEvent e) { if (!active) row.setOpaque(false); row.repaint(); }
            });
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            side.add(row);
            side.add(Box.createVerticalStrut(5));
        }

        side.add(Box.createVerticalGlue());
        haven.theme.RoundedPanel footer = new haven.theme.RoundedPanel(16, new Color(27, 48, 66), new Color(64, 83, 99));
        footer.setShadowEnabled(false);
        footer.setLayout(new BorderLayout(5, 6));
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        footer.setPreferredSize(new Dimension(190, 132));
        footer.setBorder(new EmptyBorder(12, 10, 12, 10));
        JLabel miniBrand = new JLabel("✦  HAVEN / STAYS", SwingConstants.CENTER);
        miniBrand.setForeground(HavenColors.CHAMPAGNE_GOLD);
        miniBrand.setFont(new Font("Serif", Font.BOLD, 15));
        JLabel comfort = new JLabel("Luxury Stay in Comfort", SwingConstants.CENTER);
        comfort.setForeground(new Color(232, 238, 243));
        comfort.setFont(new Font("Serif", Font.ITALIC, 12));
        JLabel note = new JLabel("A better guest experience", SwingConstants.CENTER);
        note.setForeground(new Color(160, 178, 192));
        note.setFont(new Font("SansSerif", Font.PLAIN, 9));
        JPanel footerText = new JPanel(new GridLayout(3, 1, 0, 3));
        footerText.setOpaque(false);
        footerText.add(miniBrand); footerText.add(comfort); footerText.add(note);
        footer.add(new SkylineMark(), BorderLayout.NORTH);
        footer.add(footerText, BorderLayout.CENTER);
        side.add(footer);
        return side;
    }

    private void addSearchField(JPanel panel, int column, String label, JComponent field) {
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = column; lc.gridy = 0; lc.anchor = GridBagConstraints.WEST;
        lc.insets = new Insets(0, 7, 5, 7);
        JLabel l = new JLabel(label);
        l.setForeground(new Color(84, 104, 123));
        l.setFont(new Font("SansSerif", Font.BOLD, 9));
        panel.add(l, lc);
        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = column; fc.gridy = 1; fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1; fc.insets = new Insets(0, 7, 0, 7);
        field.setFont(new Font("SansSerif", Font.PLAIN, 12));
        if (field instanceof JTextField tf)
            tf.setBorder(new CompoundBorder(new LineBorder(new Color(205, 218, 230)), new EmptyBorder(8, 8, 8, 8)));
        panel.add(field, fc);
    }

    private JPanel portalRoomCard(String name, String description, String price, String category,
                                  String rooms, String imageName, String roomNumber) {
        JPanel card = new RoundedPanel(16, Color.WHITE);
        card.setLayout(new BorderLayout(16, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(ThemePalette.BORDER), new EmptyBorder(14, 15, 14, 15)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 154));
        JLabel photo = new JLabel();
        photo.setPreferredSize(new Dimension(190, 126));
        photo.setHorizontalAlignment(SwingConstants.CENTER);
        photo.setOpaque(true);
        photo.setBackground(new Color(30, 67, 82));
        java.net.URL resource = getClass().getResource("/images/" + imageName);
        if (resource != null) {
            ImageIcon source = new ImageIcon(resource);
            Image scaled = source.getImage().getScaledInstance(190, 126, Image.SCALE_SMOOTH);
            photo.setIcon(new ImageIcon(scaled));
        } else {
            photo.setText("HAVEN HOTEL");
            photo.setForeground(Color.WHITE);
        }
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(name);
        title.setForeground(NAVY);
        title.setFont(new Font("SansSerif", Font.BOLD, 17));
        JLabel stars = new JLabel("★★★★★   •   " + category);
        stars.setForeground(new Color(222, 145, 35));
        stars.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel desc = new JLabel("<html><div style='width:300px;color:#5b6d7d'>" + description + "</div></html>");
        desc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JLabel roomInfo = new JLabel("ROOMS " + rooms + "   •   COMFORT-LED DESIGN");
        roomInfo.setForeground(new Color(105, 121, 136));
        roomInfo.setFont(new Font("SansSerif", Font.BOLD, 9));
        JLabel amenities = new JLabel("WI-FI   •   AIR CONDITIONING   •   ENSUITE");
        amenities.setForeground(ThemePalette.DEEP_TEAL);
        amenities.setFont(new Font("SansSerif", Font.BOLD, 9));
        details.add(title);
        details.add(Box.createVerticalStrut(5));
        details.add(stars);
        details.add(Box.createVerticalStrut(9));
        details.add(desc);
        details.add(Box.createVerticalStrut(7));
        details.add(roomInfo);
        details.add(Box.createVerticalStrut(5));
        details.add(amenities);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel rate = new JLabel(price);
        rate.setForeground(NAVY);
        rate.setFont(new Font("SansSerif", Font.BOLD, 21));
        JLabel per = new JLabel("per night");
        per.setForeground(new Color(107, 121, 135));
        JButton reserve = new RoundedButton("SELECT ROOM", BLUE);
        reserve.addActionListener(e -> {
            for (int i = 0; i < roomCombo.getItemCount(); i++) {
                String item = roomCombo.getItemAt(i);
                if (item.contains(" - " + roomNumber + " ")) { roomCombo.setSelectedIndex(i); break; }
            }
            JTabbedPane tabs = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, card);
            if (tabs != null) tabs.setSelectedIndex(1);
            statusLabel.setText(name + " selected. Complete the guest details to reserve it.");
        });
        right.add(rate);
        right.add(per);
        right.add(Box.createVerticalStrut(12));
        right.add(reserve);
        card.add(photo, BorderLayout.WEST);
        card.add(details, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel metricCard(String label, JLabel value, Color accent) {
        JPanel card = new RoundedPanel(16, Color.WHITE);
        card.setLayout(new BorderLayout(5, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(ThemePalette.BORDER), new EmptyBorder(18, 20, 18, 20)));
        JLabel name = new JLabel(label);
        name.setForeground(new Color(105, 119, 133));
        name.setFont(new Font("SansSerif", Font.BOLD, 11));
        value.setForeground(accent);
        value.setFont(new Font("SansSerif", Font.BOLD, 34));
        card.add(name, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBookingPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(ThemePalette.SOFT_ALABASTER);
        outer.setBorder(new EmptyBorder(22, 22, 22, 22));
        JPanel card = new RoundedPanel(16, Color.WHITE);
        card.setLayout(new BorderLayout(15, 15));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(ThemePalette.BORDER), new EmptyBorder(26, 30, 26, 30)));

        JLabel heading = new JLabel("Create a reservation");
        heading.setFont(new Font("SansSerif", Font.BOLD, 21));
        heading.setForeground(NAVY);
        JLabel description = new JLabel("Enter guest details and select a room for the requested dates.");
        description.setForeground(new Color(105, 119, 133));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        titlePanel.setOpaque(false);
        titlePanel.add(heading);
        titlePanel.add(description);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        addFormRow(form, 0, "Guest full name", guestField);
        addFormRow(form, 1, "Phone number", phoneField);
        addFormRow(form, 2, "Email address", emailField);
        addFormRow(form, 3, "Room", roomCombo);
        addFormRow(form, 4, "Check-in (YYYY-MM-DD)", checkInField);
        addFormRow(form, 5, "Check-out (YYYY-MM-DD)", checkOutField);
        addFormRow(form, 6, "Number of guests", guestsSpinner);

        JButton save = new RoundedButton("CONFIRM RESERVATION", GREEN);
        save.addActionListener(e -> createReservation());
        JButton clear = new RoundedButton("CLEAR FORM", new Color(105, 119, 133));
        clear.addActionListener(e -> clearForm());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.add(save);
        actions.add(clear);

        card.add(titlePanel, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        outer.add(card, gbc);
        return outer;
    }

    private void addFormRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0; left.gridy = row; left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(8, 0, 8, 20);
        JLabel l = new JLabel(label);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(new Color(65, 80, 95));
        form.add(l, left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1; right.gridy = row; right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(8, 0, 8, 0);
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.getAccessibleContext().setAccessibleName(label);
        if (field instanceof JTextField tf) {
            tf.setBackground(Color.WHITE);
            tf.setForeground(ThemePalette.MIDNIGHT_SLATE);
            tf.setBorder(new CompoundBorder(new LineBorder(ThemePalette.BORDER), new EmptyBorder(10, 11, 10, 11)));
        }
        form.add(field, right);
    }

    private JPanel buildReservationsPanel() {
        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setBackground(new Color(238, 242, 245));
        page.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel titleBanner = new JPanel(new BorderLayout());
        titleBanner.setBackground(NAVY);
        titleBanner.setBorder(new EmptyBorder(10, 18, 10, 18));
        JLabel title = new JLabel("RESERVATION");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Serif", Font.BOLD, 29));
        JLabel subtitle = new JLabel("GUEST DETAILS  •  ROOM SELECTION  •  CONFIRMATION");
        subtitle.setForeground(HavenColors.CHAMPAGNE_GOLD);
        subtitle.setFont(new Font("SansSerif", Font.BOLD, 9));
        JPanel titleText = new JPanel(); titleText.setOpaque(false);
        titleText.setLayout(new BoxLayout(titleText, BoxLayout.Y_AXIS));
        titleText.add(title); titleText.add(Box.createVerticalStrut(3)); titleText.add(subtitle);
        titleBanner.add(titleText, BorderLayout.WEST);

        JPanel customer = new JPanel(new BorderLayout());
        customer.setBackground(NAVY);
        customer.setBorder(new LineBorder(new Color(203,213,225), 1));
        customer.add(sectionBar("CUSTOMER INFORMATION"), BorderLayout.NORTH);
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBackground(NAVY);
        fields.setBorder(new EmptyBorder(14, 17, 16, 17));
        reservationName = underlineField("Full Name");
        reservationPhone = underlineField("Primary Phone");
        reservationEmail = underlineField("Email Address");
        reservationIdNumber = underlineField("Passport / ID Number");
        reservationSecondaryPhone = underlineField("Secondary Phone");
        immediateCheckIn = new JCheckBox("Immediate Check-in");
        immediateCheckIn.setOpaque(false); immediateCheckIn.setForeground(new Color(226,232,240));
        immediateCheckIn.setFont(new Font("SansSerif", Font.BOLD, 12));
        addCustomerField(fields, 0, 0, "FULL NAME", reservationName);
        addCustomerField(fields, 1, 0, "PRIMARY PHONE", reservationPhone);
        addCustomerField(fields, 2, 0, "EMAIL ADDRESS", reservationEmail);
        addCustomerField(fields, 0, 1, "PASSPORT / ID NUMBER", reservationIdNumber);
        addCustomerField(fields, 1, 1, "SECONDARY PHONE", reservationSecondaryPhone);
        GridBagConstraints check = new GridBagConstraints(); check.gridx=2; check.gridy=1; check.weightx=1;
        check.fill=GridBagConstraints.HORIZONTAL; check.anchor=GridBagConstraints.CENTER; check.insets=new Insets(13,12,5,12);
        fields.add(immediateCheckIn, check);
        customer.add(fields, BorderLayout.CENTER);

        JPanel roomSection = new JPanel(new BorderLayout(0, 0));
        roomSection.setBackground(Color.WHITE);
        roomSection.setBorder(new LineBorder(new Color(203,213,225),1));
        roomSection.add(sectionBar("ROOM INFORMATION"), BorderLayout.NORTH);
        reservationRoomModel = new DefaultTableModel(new Object[]{"Room", "Type", "Days", "Per Day", "Extra Bed", "Per Extra Bed", "Person", "Check-in Date", "Check-out Date", "Total"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return col != 9 && col != 1; }
            @Override public Class<?> getColumnClass(int col) { return (col==2 || col==4 || col==6) ? Integer.class : (col==3 || col==5 || col==9) ? Double.class : String.class; }
        };
        reservationRoomTable = new JTable(reservationRoomModel);
        reservationRoomTable.setRowHeight(40); reservationRoomTable.setFont(new Font("SansSerif",Font.PLAIN,12));
        reservationRoomTable.setForeground(NAVY); reservationRoomTable.setBackground(Color.WHITE);
        reservationRoomTable.setGridColor(new Color(203,213,225)); reservationRoomTable.setShowVerticalLines(true);
        reservationRoomTable.setSelectionBackground(new Color(226,232,240));
        reservationRoomTable.setSelectionForeground(NAVY);
        reservationRoomTable.getTableHeader().setBackground(new Color(248,250,252));
        reservationRoomTable.getTableHeader().setForeground(NAVY);
        reservationRoomTable.getTableHeader().setFont(new Font("Serif",Font.BOLD,13));
        reservationRoomTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths={92,145,78,106,112,148,96,175,175,110};
        for(int i=0;i<widths.length;i++) reservationRoomTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        if (roomCombo.getItemCount()>0) {
            JComboBox<String> roomEditor = new JComboBox<>();
            for(int i=0;i<roomCombo.getItemCount();i++) roomEditor.addItem(roomCombo.getItemAt(i));
            reservationRoomTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(roomEditor));
        }
        reservationRoomTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean focus,int r,int c){
                JLabel l=(JLabel)super.getTableCellRendererComponent(t,v,sel,focus,r,c); l.setForeground(new Color(30,78,95)); l.setFont(new Font("SansSerif",Font.BOLD,10)); return l;
            }
        });
        reservationRoomModel.addTableModelListener(e -> {
            if(e.getType()==javax.swing.event.TableModelEvent.UPDATE && e.getColumn()==0 && e.getFirstRow()>=0){
                int r=e.getFirstRow();String selected=String.valueOf(reservationRoomModel.getValueAt(r,0));
                int a=selected.indexOf('('),b=selected.indexOf(')');if(a>=0&&b>a)reservationRoomModel.setValueAt(selected.substring(a+1,b),r,1);
                int dollar=selected.lastIndexOf('$');if(dollar>=0)try{reservationRoomModel.setValueAt(Double.parseDouble(selected.substring(dollar+1)),r,3);}catch(Exception ignored){}
            }
            if(e.getType()==javax.swing.event.TableModelEvent.INSERT || e.getType()==javax.swing.event.TableModelEvent.DELETE || (e.getType()==javax.swing.event.TableModelEvent.UPDATE && e.getColumn()!=9)) recalculateReservationTotals();
        });
        immediateCheckIn.addActionListener(e -> {
            if(immediateCheckIn.isSelected() && reservationRoomModel!=null){
                for(int r=0;r<reservationRoomModel.getRowCount();r++){
                    reservationRoomModel.setValueAt(LocalDate.now().toString(),r,7);
                    try{LocalDate out=LocalDate.parse(String.valueOf(reservationRoomModel.getValueAt(r,8)));int nights=(int)Math.max(1,java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(),out));reservationRoomModel.setValueAt(nights,r,2);}catch(Exception ignored){}
                }
            }
        });
        JScrollPane tableScroll = new JScrollPane(reservationRoomTable);
        tableScroll.setBorder(null); tableScroll.getViewport().setBackground(Color.WHITE);
        roomSection.add(tableScroll, BorderLayout.CENTER);

        JPanel actionBar = new JPanel(new BorderLayout(10,0));
        actionBar.setBackground(new Color(226,232,240));
        actionBar.setBorder(new EmptyBorder(9,12,9,12));
        JPanel rowActions = new JPanel(new FlowLayout(FlowLayout.LEFT,8,0)); rowActions.setOpaque(false);
        JButton add = circularAction("+", new Color(30,78,95));
        JButton remove = circularAction("−", new Color(30,78,95));
        add.setToolTipText("Add room row"); remove.setToolTipText("Remove selected room row");
        add.addActionListener(e -> addReservationRoomRow());
        remove.addActionListener(e -> { int r=reservationRoomTable.getSelectedRow(); if(r>=0) reservationRoomModel.removeRow(r); else if(reservationRoomModel.getRowCount()>0) reservationRoomModel.removeRow(reservationRoomModel.getRowCount()-1); });
        rowActions.add(add); rowActions.add(remove);
        reservationGrandTotal = new JLabel("GRAND TOTAL  $0.00");
        reservationGrandTotal.setForeground(NAVY); reservationGrandTotal.setFont(new Font("Serif",Font.BOLD,22));
        JButton confirm = new RoundedButton("✓  CONFIRM RESERVATION", new Color(30,78,95));
        confirm.setForeground(Color.WHITE); confirm.setFont(new Font("SansSerif",Font.BOLD,13));
        confirm.addActionListener(e -> saveDetailedReservation());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,14,0)); right.setOpaque(false);
        right.add(reservationGrandTotal); right.add(confirm);
        actionBar.add(rowActions,BorderLayout.WEST); actionBar.add(right,BorderLayout.EAST);
        roomSection.add(actionBar,BorderLayout.SOUTH);

        JPanel upper = new JPanel(new BorderLayout(0,8)); upper.setOpaque(false);
        upper.add(titleBanner,BorderLayout.NORTH); upper.add(customer,BorderLayout.CENTER);
        JPanel center = new JPanel(new BorderLayout(0,10)); center.setOpaque(false);
        center.add(upper,BorderLayout.NORTH); center.add(roomSection,BorderLayout.CENTER);
        page.add(center,BorderLayout.CENTER);
        addReservationRoomRow();
        return page;
    }

    private JPanel sectionBar(String text) {
        JPanel bar = new JPanel(new BorderLayout()); bar.setBackground(HavenColors.CHAMPAGNE_GOLD);
        bar.setBorder(new EmptyBorder(9,15,9,15));
        JLabel label = new JLabel(text); label.setForeground(NAVY); label.setFont(new Font("SansSerif",Font.BOLD,12));
        bar.add(label,BorderLayout.WEST); return bar;
    }

    private JTextField underlineField(String placeholder) {
        JTextField field = new PlaceholderField(placeholder); field.setOpaque(false); field.setForeground(new Color(226,232,240));
        field.setCaretColor(HavenColors.CHAMPAGNE_GOLD); field.setFont(new Font("SansSerif",Font.PLAIN,13));
        field.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(203,213,225)));
        field.setToolTipText(placeholder);
        field.putClientProperty("JTextField.placeholderText", placeholder);
        field.addFocusListener(new FocusAdapter(){@Override public void focusGained(FocusEvent e){field.setBorder(BorderFactory.createMatteBorder(0,0,2,0,HavenColors.CHAMPAGNE_GOLD));}@Override public void focusLost(FocusEvent e){field.setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(203,213,225)));}});
        return field;
    }

    private static class PlaceholderField extends JTextField {
        private final String hint;
        PlaceholderField(String hint){this.hint=hint;setOpaque(false);}
        @Override protected void paintComponent(Graphics graphics){
            super.paintComponent(graphics);
            if(getText().isEmpty()&&!isFocusOwner()){
                Graphics2D g=(Graphics2D)graphics.create();g.setColor(new Color(190,202,214,165));
                g.setFont(getFont().deriveFont(Font.ITALIC));Insets i=getInsets();
                g.drawString(hint,i.left+2,getHeight()/2+g.getFontMetrics().getAscent()/2-2);g.dispose();
            }
        }
    }

    private void addCustomerField(JPanel panel,int col,int row,String label,JComponent field){
        GridBagConstraints c=new GridBagConstraints(); c.gridx=col;c.gridy=row*2;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(2,12,2,12);
        JLabel l=new JLabel(label);l.setForeground(new Color(200,169,110));l.setFont(new Font("SansSerif",Font.BOLD,9));panel.add(l,c);
        c.gridy=row*2+1;c.insets=new Insets(0,12,9,12);field.setPreferredSize(new Dimension(180,29));panel.add(field,c);
    }

    private JButton circularAction(String text,Color color){
        JButton b=new JButton(text){@Override protected void paintComponent(Graphics g){Graphics2D x=(Graphics2D)g.create();x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);x.setColor(getModel().isPressed()?color.darker():color);x.fillOval(1,1,getWidth()-3,getHeight()-3);x.dispose();super.paintComponent(g);}};
        b.setPreferredSize(new Dimension(34,34));b.setForeground(Color.WHITE);b.setFont(new Font("SansSerif",Font.BOLD,20));b.setBorderPainted(false);b.setContentAreaFilled(false);b.setFocusPainted(false);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));return b;
    }

    private void addReservationRoomRow(){
        if(reservationRoomModel==null)return;
        String room=roomCombo.getItemCount()>0?roomCombo.getItemAt(0):"";
        String type=""; double rate=0;
        if(!room.isEmpty()){int a=room.indexOf('('),b=room.indexOf(')');if(a>=0&&b>a)type=room.substring(a+1,b);int dollar=room.lastIndexOf('$');if(dollar>=0)try{rate=Double.parseDouble(room.substring(dollar+1));}catch(Exception ignored){}}
        reservationRoomModel.addRow(new Object[]{room,type,2,rate,0,0.0,1,LocalDate.now().toString(),LocalDate.now().plusDays(2).toString(),rate*2});
        recalculateReservationTotals();
    }

    private void recalculateReservationTotals(){
        if(reservationRoomModel==null||reservationGrandTotal==null)return;
        double grand=0;
        for(int r=0;r<reservationRoomModel.getRowCount();r++)try{
            int days=Integer.parseInt(String.valueOf(reservationRoomModel.getValueAt(r,2)));
            double per=Double.parseDouble(String.valueOf(reservationRoomModel.getValueAt(r,3)));
            int extra=Integer.parseInt(String.valueOf(reservationRoomModel.getValueAt(r,4)));
            double extraRate=Double.parseDouble(String.valueOf(reservationRoomModel.getValueAt(r,5)));
            double total=Math.max(0,days)*Math.max(0,per)+Math.max(0,days)*Math.max(0,extra)*Math.max(0,extraRate);
            reservationRoomModel.setValueAt(total,r,9);grand+=total;
        }catch(Exception ignored){}
        reservationGrandTotal.setText(String.format("Grand Total:  $%.2f",grand));
    }

    private void saveDetailedReservation(){
        if(reservationRoomTable.isEditing())reservationRoomTable.getCellEditor().stopCellEditing();
        String name=reservationName.getText().trim(),phone=reservationPhone.getText().trim();
        if(name.isEmpty()||phone.isEmpty()){showError("Full Name and Primary Phone are required.");return;}
        if(reservationRoomModel.getRowCount()==0){showError("Add at least one room row.");return;}
        try(Connection c=connect()){
            c.setAutoCommit(false);
            try{
                int guestId;
                try(PreparedStatement p=c.prepareStatement("INSERT INTO guests(full_name,phone,email,secondary_phone,id_number) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                    p.setString(1,name);p.setString(2,phone);p.setString(3,reservationEmail.getText().trim());
                    p.setString(4,reservationSecondaryPhone.getText().trim());p.setString(5,reservationIdNumber.getText().trim());p.executeUpdate();
                    try(ResultSet keys=p.getGeneratedKeys()){if(!keys.next())throw new SQLException("Guest record could not be created.");guestId=keys.getInt(1);}
                }
                int guestCount=0;
                for(int r=0;r<reservationRoomModel.getRowCount();r++){
                    String roomText=String.valueOf(reservationRoomModel.getValueAt(r,0));
                    int split=roomText.indexOf(" - ");if(split<1)throw new SQLException("Choose a valid room for every row.");
                    int roomId=Integer.parseInt(roomText.substring(0,split));
                    LocalDate in=LocalDate.parse(String.valueOf(reservationRoomModel.getValueAt(r,7)));
                    LocalDate out=LocalDate.parse(String.valueOf(reservationRoomModel.getValueAt(r,8)));
                    if(!out.isAfter(in))throw new SQLException("Check-out must be after check-in for each room.");
                    try(PreparedStatement q=c.prepareStatement("SELECT COUNT(*) FROM reservations WHERE room_id=? AND status='CONFIRMED' AND NOT(check_out<=? OR check_in>=?)")){
                        q.setInt(1,roomId);q.setString(2,in.toString());q.setString(3,out.toString());try(ResultSet rs=q.executeQuery()){if(rs.next()&&rs.getInt(1)>0)throw new SQLException("Room " + roomText + " is already reserved for the selected dates.");}
                    }
                    int persons=Integer.parseInt(String.valueOf(reservationRoomModel.getValueAt(r,6)));guestCount+=persons;
                    int days=Integer.parseInt(String.valueOf(reservationRoomModel.getValueAt(r,2)));
                    double nightly=Double.parseDouble(String.valueOf(reservationRoomModel.getValueAt(r,3)));
                    int extraBeds=Integer.parseInt(String.valueOf(reservationRoomModel.getValueAt(r,4)));
                    double extraRate=Double.parseDouble(String.valueOf(reservationRoomModel.getValueAt(r,5)));
                    double total=Double.parseDouble(String.valueOf(reservationRoomModel.getValueAt(r,9)));
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO reservations(guest_id,room_id,check_in,check_out,guests,status,immediate_checkin,days,nightly_rate,extra_bed,extra_bed_rate,total_amount) VALUES(?,?,?,?,?,'CONFIRMED',?,?,?,?,?,?)")){
                        p.setInt(1,guestId);p.setInt(2,roomId);p.setString(3,in.toString());p.setString(4,out.toString());p.setInt(5,Math.max(1,persons));
                        p.setInt(6,immediateCheckIn.isSelected()?1:0);p.setInt(7,days);p.setDouble(8,nightly);p.setInt(9,extraBeds);p.setDouble(10,extraRate);p.setDouble(11,total);p.executeUpdate();
                    }
                }
                c.commit();refreshAll();
                JOptionPane.showMessageDialog(this,"Reservation confirmed for "+name+".\n"+reservationGrandTotal.getText(),"Reservation saved",JOptionPane.INFORMATION_MESSAGE);
                statusLabel.setText("Reservation confirmed successfully.");
                reservationName.setText("");reservationPhone.setText("");reservationEmail.setText("");reservationIdNumber.setText("");reservationSecondaryPhone.setText("");immediateCheckIn.setSelected(false);
                reservationRoomModel.setRowCount(0);addReservationRoomRow();
            }catch(Exception ex){c.rollback();showError(ex.getMessage());}finally{c.setAutoCommit(true);}
        }catch(SQLException ex){showError("Database error: "+ex.getMessage());}
    }

    private JPanel buildRoomsPanel() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setBackground(new Color(238, 242, 245));
        page.setBorder(new EmptyBorder(16, 18, 16, 18));

        RoomShowcase[] rooms = {
                new RoomShowcase("The Haven Standard", "Standard", "$55", "standard.jpg", "101", "Queen bed  •  Wi-Fi  •  Ensuite", "A calm, beautifully prepared retreat with warm lighting and all the essentials."),
                new RoomShowcase("Deluxe Ocean View", "Deluxe", "$85", "deluxe.jpg", "201", "King bed  •  Balcony  •  Wi-Fi", "A spacious room with refined finishes and a relaxing view."),
                new RoomShowcase("Executive Suite", "Family Suite", "$130+", "suite.jpg", "301", "Lounge  •  Ensuite  •  Breakfast", "Extra room to unwind, ideal for longer stays and family escapes."),
                new RoomShowcase("The Penthouse", "Executive Suite", "$175", "penthouse.jpg", "302", "Panoramic view  •  Lounge  •  Premium", "An elevated coastal stay with generous space and premium comfort.")
        };

        JPanel hero = new JPanel(new BorderLayout(18, 0));
        hero.setPreferredSize(new Dimension(1000, 350));
        hero.setMinimumSize(new Dimension(650, 330));
        hero.setOpaque(false);
        HeroRoomPanel heroImage = new HeroRoomPanel("/images/featured_room.jpg");
        heroImage.setLayout(new BorderLayout(16, 0));
        heroImage.setBorder(new EmptyBorder(24, 28, 24, 24));

        JPanel story = new JPanel();
        story.setOpaque(false);
        story.setLayout(new BoxLayout(story, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("HAVEN / STAYS   •   SIGNATURE COLLECTION");
        eyebrow.setForeground(new Color(220, 190, 132));
        eyebrow.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel featureTitle = new JLabel("The Haven Standard");
        featureTitle.setForeground(Color.WHITE);
        featureTitle.setFont(new Font("Serif", Font.BOLD, 31));
        JLabel featureRate = new JLabel("$55  /  NIGHT");
        featureRate.setForeground(new Color(225, 201, 151));
        featureRate.setFont(new Font("SansSerif", Font.BOLD, 18));
        JLabel featureDescription = new JLabel("<html><div style='width:390px;color:#edf2f6'>A calm, beautifully prepared retreat with warm lighting and all the essentials.</div></html>");
        featureDescription.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JLabel featureAmenities = new JLabel("✦  Queen bed     ✦  Wi-Fi     ✦  Ensuite");
        featureAmenities.setForeground(new Color(232, 238, 243));
        featureAmenities.setFont(new Font("SansSerif", Font.BOLD, 11));
        story.add(Box.createVerticalGlue()); story.add(eyebrow); story.add(Box.createVerticalStrut(12));
        story.add(featureTitle); story.add(Box.createVerticalStrut(8)); story.add(featureRate);
        story.add(Box.createVerticalStrut(13)); story.add(featureDescription);
        story.add(Box.createVerticalStrut(15)); story.add(featureAmenities); story.add(Box.createVerticalGlue());
        heroImage.add(story, BorderLayout.CENTER);

        JPanel booking = new GlassBookingCard();
        booking.setPreferredSize(new Dimension(310, 300));
        booking.setLayout(new BorderLayout(0, 9));
        booking.setBorder(new EmptyBorder(0, 0, 13, 0));
        JLabel bookingTitle = new JLabel("  BOOK YOUR STAY", SwingConstants.LEFT);
        bookingTitle.setOpaque(true);
        bookingTitle.setBackground(new Color(200, 169, 110));
        bookingTitle.setForeground(new Color(15, 30, 46));
        bookingTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
        bookingTitle.setBorder(new EmptyBorder(12, 12, 12, 8));
        booking.add(bookingTitle, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(0, 12, 0, 12));
        JComboBox<String> typeSelect = new JComboBox<>(new String[]{"Standard", "Deluxe", "Family Suite", "Executive Suite"});
        JTextField inDate = new JTextField(LocalDate.now().toString());
        JTextField outDate = new JTextField(LocalDate.now().plusDays(2).toString());
        JSpinner adults = new JSpinner(new SpinnerNumberModel(2, 1, 8, 1));
        JSpinner children = new JSpinner(new SpinnerNumberModel(0, 0, 6, 1));
        JLabel nights = new JLabel("2 nights");
        nights.setForeground(new Color(232, 238, 243));
        styleRoomInput(typeSelect); styleRoomInput(inDate); styleRoomInput(outDate); styleRoomInput(adults); styleRoomInput(children);
        addRoomFormRow(form, 0, "ROOM TYPE", typeSelect);
        addRoomFormRow(form, 1, "CHECK-IN  (YYYY-MM-DD)", inDate);
        addRoomFormRow(form, 2, "CHECK-OUT  (YYYY-MM-DD)", outDate);
        JPanel guestRow = new JPanel(new GridLayout(1, 2, 8, 0)); guestRow.setOpaque(false);
        JPanel adultBox = new JPanel(new BorderLayout(0, 3)); adultBox.setOpaque(false);
        JLabel adultLabel = new JLabel("ADULTS"); adultLabel.setForeground(new Color(207, 218, 228)); adultLabel.setFont(new Font("SansSerif", Font.BOLD, 9));
        adultBox.add(adultLabel, BorderLayout.NORTH); adultBox.add(adults);
        JPanel childBox = new JPanel(new BorderLayout(0, 3)); childBox.setOpaque(false);
        JLabel childLabel = new JLabel("CHILDREN"); childLabel.setForeground(new Color(207, 218, 228)); childLabel.setFont(new Font("SansSerif", Font.BOLD, 9));
        childBox.add(childLabel, BorderLayout.NORTH); childBox.add(children);
        guestRow.add(adultBox); guestRow.add(childBox);
        GridBagConstraints gc = new GridBagConstraints(); gc.gridx=0; gc.gridy=6; gc.gridwidth=2; gc.fill=GridBagConstraints.HORIZONTAL; gc.weightx=1; gc.insets=new Insets(5,0,3,0); form.add(guestRow,gc);
        JPanel nightsRow = new JPanel(new BorderLayout()); nightsRow.setOpaque(false);
        JLabel nightLabel = new JLabel("STAY LENGTH"); nightLabel.setForeground(new Color(207,218,228)); nightLabel.setFont(new Font("SansSerif",Font.BOLD,9));
        nightsRow.add(nightLabel,BorderLayout.WEST); nightsRow.add(nights,BorderLayout.EAST);
        gc.gridy=7; gc.insets=new Insets(4,0,3,0); form.add(nightsRow,gc);
        Runnable updateNights = () -> { try { long n=java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(inDate.getText().trim()),LocalDate.parse(outDate.getText().trim())); nights.setText(n>0?n+" nights":"Check dates"); } catch(Exception ex){ nights.setText("Check dates"); } };
        inDate.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){updateNights.run();}public void removeUpdate(javax.swing.event.DocumentEvent e){updateNights.run();}public void changedUpdate(javax.swing.event.DocumentEvent e){updateNights.run();}});
        outDate.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){updateNights.run();}public void removeUpdate(javax.swing.event.DocumentEvent e){updateNights.run();}public void changedUpdate(javax.swing.event.DocumentEvent e){updateNights.run();}});
        JButton searchRooms = new RoundedButton("SEARCH ROOMS", new Color(30,78,95));
        searchRooms.setFont(new Font("SansSerif",Font.BOLD,12));
        searchRooms.addActionListener(e -> {
            try {
                LocalDate ci=LocalDate.parse(inDate.getText().trim()), co=LocalDate.parse(outDate.getText().trim());
                if(!co.isAfter(ci)) throw new IllegalArgumentException("Check-out must be after check-in.");
                String chosen=(String)typeSelect.getSelectedItem(); int count=0;
                try(Connection c=connect(); PreparedStatement q=c.prepareStatement("""
                    SELECT COUNT(*) FROM rooms rm WHERE rm.room_type=? AND NOT EXISTS (
                      SELECT 1 FROM reservations r WHERE r.room_id=rm.id AND r.status='CONFIRMED'
                      AND NOT (r.check_out<=? OR r.check_in>=?))
                    """)) { q.setString(1,chosen); q.setString(2,ci.toString()); q.setString(3,co.toString()); try(ResultSet rs=q.executeQuery()){if(rs.next())count=rs.getInt(1);} }
                JOptionPane.showMessageDialog(this,count+" available room(s) for "+chosen+" from "+ci+" to "+co+".","Room availability",JOptionPane.INFORMATION_MESSAGE);
                statusLabel.setText(count+" available "+chosen+" room(s) found.");
            } catch(Exception ex){ showError("Please check your dates. "+ex.getMessage()); }
        });
        gc.gridy=8; gc.insets=new Insets(8,0,0,0); form.add(searchRooms,gc);
        booking.add(form,BorderLayout.CENTER);
        heroImage.add(booking,BorderLayout.EAST);
        hero.add(heroImage,BorderLayout.CENTER);

        JPanel gallery = new JPanel(new BorderLayout(0, 10));
        gallery.setOpaque(false);
        JPanel galleryHeader = new JPanel(new BorderLayout(10,0));
        galleryHeader.setBackground(new Color(15,30,46));
        galleryHeader.setBorder(new EmptyBorder(10,14,10,12));
        JLabel galleryTitle = new JLabel("FEATURED ROOMS & SUITES");
        galleryTitle.setForeground(new Color(226,232,240));
        galleryTitle.setFont(new Font("SansSerif",Font.BOLD,13));
        JPanel strip=new JPanel(new FlowLayout(FlowLayout.LEFT,13,5));strip.setOpaque(false);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT,7,0)); controls.setOpaque(false);
        String[] filters={"ALL STAYS","STANDARD","DELUXE","SUITES"};
        for(String f:filters){JButton pill=new JButton(f);pill.setFocusable(false);pill.setFont(new Font("SansSerif",Font.BOLD,9));pill.setForeground(new Color(226,232,240));pill.setBackground(new Color(37,58,75));pill.setBorder(new CompoundBorder(new LineBorder(new Color(83,105,121)),new EmptyBorder(5,9,5,9)));pill.addActionListener(e->{for(Component c:controls.getComponents())if(c instanceof JButton b && b.getText()!=null && java.util.Arrays.asList(filters).contains(b.getText()))b.setBackground(b==pill?new Color(200,169,110):new Color(37,58,75));for(int i=0;i<strip.getComponentCount();i++){RoomShowcase r=rooms[i];boolean show=f.equals("ALL STAYS")||(f.equals("STANDARD")&&r.type.equals("Standard"))||(f.equals("DELUXE")&&r.type.equals("Deluxe"))||(f.equals("SUITES")&&r.type.toLowerCase().contains("suite"));strip.getComponent(i).setVisible(show);}strip.revalidate();strip.repaint();});controls.add(pill);}
        JButton left=new JButton("‹"), right=new JButton("›"); for(JButton b:new JButton[]{left,right}){b.setForeground(new Color(200,169,110));b.setBackground(new Color(15,30,46));b.setBorderPainted(false);b.setFont(new Font("SansSerif",Font.BOLD,20));controls.add(b);}
        galleryHeader.add(galleryTitle,BorderLayout.WEST);galleryHeader.add(controls,BorderLayout.EAST);
        for(RoomShowcase room:rooms){JPanel card=roomGalleryCard(room,()->{
            heroImage.setImage("/images/featured_room.jpg");
            heroImage.setImage(room.imagePath());
            featureTitle.setText(room.name); featureRate.setText(room.price+"  /  NIGHT");
            featureDescription.setText("<html><div style='width:390px;color:#edf2f6'>"+room.description+"</div></html>");
            featureAmenities.setText("✦  "+room.amenities.replace("  •  ","     ✦  "));
            typeSelect.setSelectedItem(room.type);
            for(int i=0;i<roomCombo.getItemCount();i++){String item=roomCombo.getItemAt(i);if(item.contains(" - "+room.roomNumber+" ")){roomCombo.setSelectedIndex(i);break;}}
            statusLabel.setText(room.name+" selected for your stay.");
        });strip.add(card);}
        JScrollPane horizontal=new JScrollPane(strip,ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        horizontal.setBorder(null);horizontal.setOpaque(false);horizontal.getViewport().setOpaque(false);horizontal.getHorizontalScrollBar().setUnitIncrement(24);
        left.addActionListener(e->horizontal.getHorizontalScrollBar().setValue(Math.max(0,horizontal.getHorizontalScrollBar().getValue()-260)));
        right.addActionListener(e->horizontal.getHorizontalScrollBar().setValue(horizontal.getHorizontalScrollBar().getValue()+260));
        gallery.add(galleryHeader,BorderLayout.NORTH);gallery.add(horizontal,BorderLayout.CENTER);
        JPanel content=new JPanel(new BorderLayout(0,15));content.setOpaque(false);content.add(hero,BorderLayout.CENTER);content.add(gallery,BorderLayout.SOUTH);
        page.add(content,BorderLayout.CENTER);
        return page;
    }

    private void styleRoomInput(JComponent component) {
        component.setFont(new Font("SansSerif",Font.PLAIN,11));
        component.setForeground(new Color(226,232,240));
        component.setBackground(new Color(31,49,66));
        component.setBorder(new CompoundBorder(new LineBorder(new Color(145,161,175)),new EmptyBorder(5,7,5,7)));
        if(component instanceof JSpinner spinner) ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().setForeground(new Color(226,232,240));
    }

    private void addRoomFormRow(JPanel form,int row,String label,JComponent field){
        GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=row*2;c.gridwidth=2;c.fill=GridBagConstraints.HORIZONTAL;c.weightx=1;c.insets=new Insets(4,0,1,0);
        JLabel l=new JLabel(label);l.setForeground(new Color(207,218,228));l.setFont(new Font("SansSerif",Font.BOLD,9));form.add(l,c);
        c.gridy=row*2+1;c.insets=new Insets(0,0,3,0);form.add(field,c);
    }

    private JPanel roomGalleryCard(RoomShowcase room,Runnable onSelect){
        JPanel card=new RoundedPanel(13,Color.WHITE);card.setLayout(new BorderLayout(0,7));card.setPreferredSize(new Dimension(220,190));card.setBorder(new CompoundBorder(new LineBorder(new Color(203,213,225)),new EmptyBorder(8,8,8,8)));
        JLabel image=new JLabel();image.setOpaque(true);image.setBackground(new Color(33,57,72));image.setPreferredSize(new Dimension(200,83));image.setHorizontalAlignment(SwingConstants.CENTER);
        java.net.URL url=getClass().getResource(room.imagePath());if(url!=null){ImageIcon icon=new ImageIcon(url);image.setIcon(new ImageIcon(icon.getImage().getScaledInstance(200,83,Image.SCALE_SMOOTH)));}else{image.setText("HAVEN / STAYS");image.setForeground(Color.WHITE);}
        JPanel info=new JPanel(new BorderLayout());info.setOpaque(false);JLabel name=new JLabel(room.name);name.setForeground(new Color(15,30,46));name.setFont(new Font("SansSerif",Font.BOLD,12));JLabel price=new JLabel(room.price+" / night");price.setForeground(new Color(30,78,95));price.setFont(new Font("SansSerif",Font.BOLD,12));info.add(name,BorderLayout.WEST);info.add(price,BorderLayout.EAST);
        JLabel amenities=new JLabel(room.amenities);amenities.setForeground(new Color(91,109,125));amenities.setFont(new Font("SansSerif",Font.PLAIN,9));
        JButton select=new RoundedButton("SELECT ROOM",new Color(30,78,95));select.setFont(new Font("SansSerif",Font.BOLD,10));select.addActionListener(e->onSelect.run());
        card.add(image,BorderLayout.NORTH);JPanel details=new JPanel(new BorderLayout(0,4));details.setOpaque(false);details.add(info,BorderLayout.NORTH);details.add(amenities,BorderLayout.CENTER);details.add(select,BorderLayout.SOUTH);card.add(details,BorderLayout.CENTER);return card;
    }

    private static class RoomShowcase {
        final String name,type,price,imageFile,roomNumber,amenities,description;
        RoomShowcase(String n,String t,String p,String img,String number,String a,String d){name=n;type=t;price=p;imageFile=img;roomNumber=number;amenities=a;description=d;}
        String imagePath(){return "/images/"+imageFile;}
    }

    private class HeroRoomPanel extends JPanel {
        private Image image;
        HeroRoomPanel(String path){setImage(path);}
        void setImage(String path){try{java.net.URL u=getClass().getResource(path);image=u==null?null:new ImageIcon(u).getImage();}catch(Exception ignored){}repaint();}
        @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.setColor(new Color(15,30,46));g.fillRect(0,0,getWidth(),getHeight());if(image!=null){double scale=Math.max(getWidth()/(double)image.getWidth(this),getHeight()/(double)image.getHeight(this));int w=(int)(image.getWidth(this)*scale),h=(int)(image.getHeight(this)*scale);g.drawImage(image,(getWidth()-w)/2,(getHeight()-h)/2,w,h,this);}g.setPaint(new GradientPaint(0,0,new Color(5,15,25,220),getWidth()*0.78f,0,new Color(5,15,25,25)));g.fillRect(0,0,getWidth(),getHeight());g.setPaint(new GradientPaint(0,getHeight()*0.55f,new Color(5,15,25,0),0,getHeight(),new Color(5,15,25,130)));g.fillRect(0,0,getWidth(),getHeight());g.dispose();}
    }

    private static class GlassBookingCard extends JPanel {
        GlassBookingCard(){setOpaque(false);}
        @Override protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(new Color(15,30,46,225));g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,18,18);g.setColor(new Color(203,213,225,190));g.setStroke(new BasicStroke(1));g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,18,18);g.dispose();super.paintComponent(graphics);}
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(HavenColors.MIDNIGHT_SLATE);
        bar.setBorder(new EmptyBorder(9, 20, 9, 20));
        statusLabel.setText("System ready  •  SQLite database connected");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(218, 229, 235));
        JLabel clock = new JLabel();
        clock.setFont(new Font("SansSerif", Font.BOLD, 11));
        clock.setForeground(HavenColors.CHAMPAGNE_GOLD);
        Runnable updateClock = () -> clock.setText(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("EEE, dd MMM yyyy   •   HH:mm:ss")));
        updateClock.run();
        Timer clockTimer = new Timer(1000, e -> updateClock.run());
        clockTimer.setCoalesce(true);
        clockTimer.start();
        bar.add(statusLabel, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        right.setOpaque(false);
        JLabel highlights = new JLabel("OCEANFRONT  •  FINE DINING  •  PRIVATE COMFORT");
        highlights.setFont(new Font("SansSerif", Font.BOLD, 9));
        highlights.setForeground(new Color(200,169,110));
        right.add(highlights);
        right.add(clock);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setGridColor(ThemePalette.BORDER);
        table.setSelectionBackground(new Color(232, 241, 242));
        table.setSelectionForeground(ThemePalette.MIDNIGHT_SLATE);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(ThemePalette.MIDNIGHT_SLATE);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        for (int c = 0; c < table.getColumnModel().getColumnCount(); c++) {
            String heading = table.getColumnModel().getColumn(c).getHeaderValue().toString().toLowerCase();
            if (heading.contains("status")) table.getColumnModel().getColumn(c).setCellRenderer(new StatusPillRenderer());
        }
    }

    private void createReservation() {
        String name = guestField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String selectedRoom = (String) roomCombo.getSelectedItem();
        if (name.isEmpty() || phone.isEmpty() || selectedRoom == null) {
            showError("Please enter the guest name and phone number, and select a room.");
            return;
        }
        LocalDate in, out;
        try {
            in = LocalDate.parse(checkInField.getText().trim());
            out = LocalDate.parse(checkOutField.getText().trim());
        } catch (DateTimeParseException ex) {
            showError("Dates must be valid and use YYYY-MM-DD format.");
            return;
        }
        if (!out.isAfter(in)) {
            showError("Check-out date must be after check-in date.");
            return;
        }
        int roomId = Integer.parseInt(selectedRoom.substring(0, selectedRoom.indexOf(" - ")));
        int guestCount = (Integer) guestsSpinner.getValue();

        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try {
                int capacity;
                try (PreparedStatement p = c.prepareStatement("SELECT capacity FROM rooms WHERE id=?")) {
                    p.setInt(1, roomId);
                    try (ResultSet rs = p.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Selected room no longer exists.");
                        capacity = rs.getInt(1);
                    }
                }
                if (guestCount > capacity) throw new SQLException("This room can accommodate up to " + capacity + " guest(s).");
                try (PreparedStatement p = c.prepareStatement("""
                    SELECT COUNT(*) FROM reservations
                    WHERE room_id=? AND status='CONFIRMED'
                    AND NOT (check_out<=? OR check_in>=?)
                """)) {
                    p.setInt(1, roomId);
                    p.setString(2, in.toString());
                    p.setString(3, out.toString());
                    try (ResultSet rs = p.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0)
                            throw new SQLException("That room is already booked for one or more of those dates.");
                    }
                }
                int guestId;
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO guests(full_name,phone,email) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    p.setString(1, name); p.setString(2, phone); p.setString(3, email);
                    p.executeUpdate();
                    try (ResultSet keys = p.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Could not create guest record.");
                        guestId = keys.getInt(1);
                    }
                }
                try (PreparedStatement p = c.prepareStatement("""
                    INSERT INTO reservations(guest_id,room_id,check_in,check_out,guests,status)
                    VALUES(?,?,?,?,?,'CONFIRMED')
                """)) {
                    p.setInt(1, guestId); p.setInt(2, roomId); p.setString(3, in.toString());
                    p.setString(4, out.toString()); p.setInt(5, guestCount);
                    p.executeUpdate();
                }
                c.commit();
                clearForm();
                refreshAll();
                statusLabel.setText("Reservation confirmed successfully.");
                JOptionPane.showMessageDialog(this, "Reservation confirmed for " + name + ".",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                c.rollback();
                showError(ex.getMessage());
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void cancelSelectedReservation() {
        int row = reservationTable.getSelectedRow();
        if (row < 0) {
            showError("Select a reservation from the table first.");
            return;
        }
        int id = (int) reservationModel.getValueAt(reservationTable.convertRowIndexToModel(row), 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Cancel reservation #" + id + "?", "Confirm cancellation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "UPDATE reservations SET status='CANCELLED' WHERE id=? AND status='CONFIRMED'")) {
            p.setInt(1, id);
            int changed = p.executeUpdate();
            if (changed == 0) showError("This reservation is already cancelled or cannot be found.");
            else statusLabel.setText("Reservation #" + id + " cancelled.");
            refreshAll();
        } catch (SQLException ex) {
            showError("Could not cancel reservation: " + ex.getMessage());
        }
    }

    private void clearForm() {
        guestField.setText("");
        phoneField.setText("");
        emailField.setText("");
        guestsSpinner.setValue(1);
        checkInField.setText(LocalDate.now().toString());
        checkOutField.setText(LocalDate.now().plusDays(2).toString());
        if (roomCombo.getItemCount() > 0) roomCombo.setSelectedIndex(0);
    }

    private void refreshAll() {
        loadRooms();
        loadReservations("");
        loadMetrics();
    }

    private void loadRooms() {
        roomCombo.removeAllItems();
        try (Connection c = connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id,room_number,room_type,price,capacity FROM rooms ORDER BY room_number")) {
            while (rs.next()) {
                roomCombo.addItem(rs.getInt("id") + " - " + rs.getString("room_number") +
                        " (" + rs.getString("room_type") + ") - $" + String.format("%.2f", rs.getDouble("price")));
            }
            syncReservationRoomChoices();
        } catch (SQLException ex) {
            statusLabel.setText("Could not load rooms: " + ex.getMessage());
        }
    }

    private void syncReservationRoomChoices(){
        if(reservationRoomTable==null||reservationRoomModel==null)return;
        JComboBox<String> choices=new JComboBox<>();for(int i=0;i<roomCombo.getItemCount();i++)choices.addItem(roomCombo.getItemAt(i));
        reservationRoomTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(choices));
        if(reservationRoomModel.getRowCount()==0){addReservationRoomRow();return;}
        if(roomCombo.getItemCount()>0&&String.valueOf(reservationRoomModel.getValueAt(0,0)).isBlank()){
            reservationRoomModel.setValueAt(roomCombo.getItemAt(0),0,0);
        }
        for(int r=0;r<reservationRoomModel.getRowCount();r++){
            String selected=String.valueOf(reservationRoomModel.getValueAt(r,0));
            int a=selected.indexOf('('),b=selected.indexOf(')');if(a>=0&&b>a)reservationRoomModel.setValueAt(selected.substring(a+1,b),r,1);
            int dollar=selected.lastIndexOf('$');if(dollar>=0)try{reservationRoomModel.setValueAt(Double.parseDouble(selected.substring(dollar+1)),r,3);}catch(Exception ignored){}
        }
    }

    private void loadReservations(String search) {
        if (reservationModel == null) return;
        reservationModel.setRowCount(0);
        if (dashboardRecentModel != null) dashboardRecentModel.setRowCount(0);
        String sql = """
            SELECT r.id,g.full_name,g.phone,rm.room_number,rm.room_type,
                   r.check_in,r.check_out,r.guests,r.status
            FROM reservations r
            JOIN guests g ON g.id=r.guest_id
            JOIN rooms rm ON rm.id=r.room_id
            WHERE (?='' OR g.full_name LIKE ? OR g.phone LIKE ? OR rm.room_number LIKE ? OR CAST(r.id AS TEXT) LIKE ?)
            ORDER BY r.id DESC
        """;
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            String q = "%" + search + "%";
            p.setString(1, search); p.setString(2, q); p.setString(3, q);
            p.setString(4, q); p.setString(5, q);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[]{rs.getInt("id"), rs.getString("full_name"), rs.getString("phone"),
                            rs.getString("room_number"), rs.getString("room_type"), rs.getString("check_in"),
                            rs.getString("check_out"), rs.getInt("guests"), rs.getString("status")};
                    reservationModel.addRow(row);
                    if (dashboardRecentModel != null && dashboardRecentModel.getRowCount() < 4)
                        dashboardRecentModel.addRow(new Object[]{row[1], row[3] + " - " + row[4], row[5], row[6], row[8]});
                }
            }
        } catch (SQLException ex) {
            statusLabel.setText("Could not load reservations: " + ex.getMessage());
        }
    }

    private void loadMetrics() {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            ensureColumn(c, "guests", "secondary_phone", "TEXT");
            ensureColumn(c, "guests", "id_number", "TEXT");
            ensureColumn(c, "reservations", "immediate_checkin", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(c, "reservations", "days", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(c, "reservations", "nightly_rate", "REAL NOT NULL DEFAULT 0");
            ensureColumn(c, "reservations", "extra_bed", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(c, "reservations", "extra_bed_rate", "REAL NOT NULL DEFAULT 0");
            ensureColumn(c, "reservations", "total_amount", "REAL NOT NULL DEFAULT 0");
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM rooms")) {
                if (rs.next()) roomsMetric.setText(String.valueOf(rs.getInt(1)));
            }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM reservations WHERE status='CONFIRMED'")) {
                if (rs.next()) bookingsMetric.setText(String.valueOf(rs.getInt(1)));
            }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM guests")) {
                if (rs.next()) guestsMetric.setText(String.valueOf(rs.getInt(1)));
            }
            try (Connection c2 = connect(); PreparedStatement p = c2.prepareStatement(
                    "SELECT COUNT(DISTINCT room_id) FROM reservations WHERE status='CONFIRMED' AND check_in<=? AND check_out>?")) {
                p.setString(1, LocalDate.now().toString());
                p.setString(2, LocalDate.now().toString());
                try (ResultSet rs = p.executeQuery()) {
                    if (rs.next()) occupiedMetric.setText(String.valueOf(rs.getInt(1)));
                }
            }
        } catch (SQLException ex) {
            statusLabel.setText("Could not load dashboard metrics: " + ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message == null ? "An unexpected error occurred." : message,
                "Hotel Reservation System", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Hero banner and overlapping KPI strip for the dashboard workspace.
     * Place /images/lobby-banner.jpg in resources for the intended lobby photograph;
     * if it is absent, the bundled suite photograph is used safely as a visual fallback.
     */
    private class DashboardHeaderPanel extends JPanel {
        private final java.awt.image.BufferedImage lobbyImage;
        private final JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));

        DashboardHeaderPanel() {
            setOpaque(false);
            setLayout(null);
            setPreferredSize(new Dimension(800, 252));
            lobbyImage = loadDashboardImage();
            metrics.setOpaque(false);
            metrics.add(dashboardMetric("TOTAL ROOMS", roomsMetric, "▦", new Color(30, 78, 95)));
            metrics.add(dashboardMetric("ACTIVE BOOKINGS", bookingsMetric, "▣", new Color(200, 169, 110)));
            metrics.add(dashboardMetric("OCCUPIED ROOMS", occupiedMetric, "⌂", new Color(47, 116, 103)));
            metrics.add(dashboardMetric("TOTAL GUESTS", guestsMetric, "♙", new Color(183, 137, 65)));
            add(metrics);
        }

        private java.awt.image.BufferedImage loadDashboardImage() {
            String[] candidates = {"/images/lobby-banner.png", "/images/lobby-banner.jpg", "/images/suite-room.png"};
            for (String candidate : candidates) {
                try {
                    java.net.URL resource = HotelReservationSystem.class.getResource(candidate);
                    if (resource != null) {
                        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(resource);
                        if (image != null) return image;
                    }
                } catch (Exception ignored) { /* Continue to the next bundled fallback. */ }
            }
            return null;
        }

        @Override public void doLayout() {
            int width = getWidth();
            int cardGap = 12;
            int cardWidth = Math.max(150, (width - 3 * cardGap) / 4);
            int rowWidth = cardWidth * 4 + cardGap * 3;
            metrics.setBounds(0, 166, Math.min(width, rowWidth), 82);
            if (width > rowWidth) metrics.setLocation((width - rowWidth) / 2, 164);
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int w = getWidth();
                int heroHeight = 194;
                java.awt.Shape clip = new java.awt.geom.RoundRectangle2D.Float(0, 0, w - 2, heroHeight, 22, 22);
                g.setClip(clip);
                g.setColor(HavenColors.ALABASTER);
                g.fillRect(0, 0, w, heroHeight);
                if (lobbyImage != null && w > 0) {
                    double scale = Math.max((double) w / lobbyImage.getWidth(), (double) heroHeight / lobbyImage.getHeight());
                    int iw = (int) Math.ceil(lobbyImage.getWidth() * scale);
                    int ih = (int) Math.ceil(lobbyImage.getHeight() * scale);
                    g.drawImage(lobbyImage, w - iw, (heroHeight - ih) / 2, iw, ih, null);
                } else {
                    // Elegant architectural fallback if no image asset can be decoded.
                    g.setPaint(new GradientPaint(w * .48f, 0, new Color(30, 78, 95), w, heroHeight, HavenColors.MIDNIGHT_SLATE));
                    g.fillRect(w / 2, 0, w / 2, heroHeight);
                    g.setColor(new Color(230, 211, 170, 95));
                    for (int i = 0; i < 4; i++) g.drawRoundRect(w - 300 + i * 62, 22, 48, 130, 8, 8);
                }
                // Left-to-right fade keeps the greeting readable while revealing the photo.
                g.setPaint(new GradientPaint(0, 0, HavenColors.ALABASTER, (float)(w * .76), 0,
                        new Color(HavenColors.ALABASTER.getRed(), HavenColors.ALABASTER.getGreen(), HavenColors.ALABASTER.getBlue(), 0)));
                g.fillRect(0, 0, w, heroHeight);
                g.setClip(null);
                g.setColor(new Color(15, 30, 46, 16));
                g.draw(clip);

                g.setColor(HavenColors.MIDNIGHT_SLATE);
                g.setFont(new Font("Serif", Font.BOLD, 30));
                g.drawString("Welcome back, Admin!", 26, 78);
                g.setColor(new Color(91, 108, 123));
                g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                g.drawString("Here's what's happening at your hotel today.", 28, 107);
                g.setColor(new Color(145, 108, 48));
                g.setFont(new Font("SansSerif", Font.BOLD, 10));
                g.drawString("HAVEN  /  STAYS     •     LUXURY HOSPITALITY", 28, 140);
            } finally { g.dispose(); }
        }
    }

    /** Luxury illustrated lobby banner; all artwork is rendered locally with Graphics2D. */
    private static class HeroBanner extends JPanel {
        HeroBanner() { setOpaque(false); setPreferredSize(new Dimension(700, 142)); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(), h=getHeight();
            g.setColor(new Color(15,30,46,24)); g.fillRoundRect(2,5,w-5,h-8,22,22);
            g.setPaint(new GradientPaint(0,0,new Color(15,30,46),w,h,new Color(35,83,91)));
            g.fillRoundRect(0,0,w-5,h-10,22,22);
            // warm architectural glow and hotel-lobby window geometry
            g.setPaint(new RadialGradientPaint(new Point(w-125, h/2), Math.max(100,h), new float[]{0f,1f}, new Color[]{new Color(200,169,110,105),new Color(200,169,110,0)}));
            g.fillRoundRect(w/2,0,w/2,h-10,22,22);
            int bx=w-280;
            g.setColor(new Color(240,225,192,85)); g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(bx,18,205,h-47,8,8); g.drawLine(bx+68,18,bx+68,h-29); g.drawLine(bx+137,18,bx+137,h-29);
            g.drawLine(bx, h/2, bx+205,h/2);
            g.setColor(new Color(10,22,31,110)); g.fillRoundRect(bx+10,h-68,185,25,8,8);
            g.setColor(ThemePalette.CHAMPAGNE_GOLD); g.fillOval(w-58,28,9,9);
            g.dispose(); super.paintComponent(graphics);
        }
    }

    /** Circular KPI icon badge. */
    private static class CircleBadge extends JLabel {
        private final Color accent;
        CircleBadge(String text, Color accent) { super(text, SwingConstants.CENTER); this.accent=accent; setForeground(Color.WHITE); setFont(new Font("SansSerif",Font.BOLD,20)); setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g=(Graphics2D)graphics.create(); g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),28)); g.fillOval(0,0,getWidth()-1,getHeight()-1);
            g.setColor(accent); g.fillOval(5,5,getWidth()-11,getHeight()-11); g.dispose(); super.paintComponent(graphics);
        }
    }

    /**
     * Animated modern background: slowly shifting blue/teal gradients and floating light orbs.
     * The animation is painted locally, so it does not require internet access or image assets.
     */
    private static class AnimatedBackground extends JPanel {
        private float phase = 0f;
        private final javax.swing.Timer animationTimer;

        AnimatedBackground() {
            setOpaque(true);
            animationTimer = new javax.swing.Timer(45, e -> {
                phase += 0.008f;
                if (phase > 1f) phase = 0f;
                repaint();
            });
            animationTimer.start();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) { g.dispose(); return; }

            float shift = (float)Math.sin(phase * Math.PI * 2);
            GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(12, 31, 58),
                    w, h, new Color(16 + (int)(12 * (shift + 1)), 91, 119));
            g.setPaint(gradient);
            g.fillRect(0, 0, w, h);

            // Soft moving light fields
            for (int i = 0; i < 5; i++) {
                double angle = phase * Math.PI * 2 + i * 1.25;
                int diameter = 180 + (i % 3) * 75;
                int x = (int)((w * (0.12 + i * 0.19)) +
                        Math.cos(angle) * (35 + i * 8)) - diameter / 2;
                int y = (int)(h * (0.35 + (i % 2) * 0.35) +
                        Math.sin(angle * 0.8) * 28) - diameter / 2;
                g.setColor(new Color(105, 210, 220, 18));
                g.fillOval(x, y, diameter, diameter);
            }

            // Floating particles
            for (int i = 0; i < 34; i++) {
                int x = (int)((i * 97L + phase * 850 * (i % 2 == 0 ? 1 : -1) + w) % w);
                int y = (int)((i * 53L + Math.sin(phase * 6 + i) * 24 + h) % h);
                int size = 2 + (i % 4);
                g.setColor(new Color(225, 248, 255, 65 + (i % 3) * 20));
                g.fillOval(x, y, size, size);
            }
            // Subtle moving light trails add depth without distracting from the controls.
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 7; i++) {
                int y = (int)(h * (0.16 + i * 0.105) + Math.sin(phase * 5 + i) * 13);
                int x = (int)((phase * 900 + i * 173) % (w + 260)) - 130;
                g.setColor(new Color(180, 235, 242, 24));
                g.drawLine(x, y, x + 110, y - 20);
            }
            g.dispose();
        }
    }

    /** Small hand-drawn vector icons for the fixed navigation rail. */
    private static class SidebarVectorIcon implements Icon {
        private final String type;
        private final Color color;
        SidebarVectorIcon(String type, Color color) { this.type = type; this.color = color; }
        @Override public int getIconWidth() { return 22; }
        @Override public int getIconHeight() { return 22; }
        @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (type) {
                case "dashboard" -> { g.drawRoundRect(2, 2, 7, 7, 2, 2); g.drawRoundRect(13, 2, 7, 7, 2, 2); g.drawRoundRect(2, 13, 7, 7, 2, 2); g.drawRoundRect(13, 13, 7, 7, 2, 2); }
                case "rooms" -> { g.drawRoundRect(2, 4, 18, 15, 3, 3); g.drawLine(5, 8, 17, 8); g.drawLine(5, 12, 17, 12); g.drawLine(5, 16, 17, 16); }
                case "reservations" -> { g.drawRoundRect(3, 3, 16, 17, 3, 3); g.drawLine(3, 8, 19, 8); g.drawLine(7, 1, 7, 5); g.drawLine(15, 1, 15, 5); g.drawLine(7, 12, 10, 15); g.drawLine(10, 15, 16, 10); }
                case "customers" -> { g.drawOval(7, 2, 8, 8); g.drawArc(3, 11, 16, 10, 0, 180); }
                case "payments" -> { g.drawRoundRect(2, 4, 18, 14, 3, 3); g.drawLine(3, 9, 19, 9); g.drawLine(6, 14, 10, 14); }
                case "reports" -> { g.drawLine(3, 19, 3, 3); g.drawLine(3, 19, 20, 19); g.drawLine(6, 16, 10, 11); g.drawLine(10, 11, 14, 14); g.drawLine(14, 14, 19, 6); }
                case "settings" -> { g.drawOval(5, 5, 12, 12); g.drawOval(9, 9, 4, 4); for (int a=0;a<8;a++){ double t=a*Math.PI/4; g.drawLine((int)(11+7*Math.cos(t)),(int)(11+7*Math.sin(t)),(int)(11+10*Math.cos(t)),(int)(11+10*Math.sin(t))); } }
                default -> { g.drawOval(3, 3, 16, 16); g.setFont(new Font("SansSerif", Font.BOLD, 14)); g.drawString("i", 9, 16); }
            }
            g.dispose();
        }
    }

    /** Custom Graphics2D skyline brand mark. */
    private static class SkylineMark extends JComponent {
        SkylineMark() { setPreferredSize(new Dimension(54, 48)); setOpaque(false); }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(ThemePalette.CHAMPAGNE_GOLD);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int base = getHeight() - 7;
            g.drawLine(3, base, 51, base);
            g.drawRect(7, 20, 8, base - 20);
            g.drawRect(18, 10, 10, base - 10);
            g.drawRect(31, 16, 8, base - 16);
            g.drawRect(42, 24, 6, base - 24);
            g.dispose();
        }
    }

    /** Paints compact rounded status pills; confirmed reservations use the requested green palette. */
    private static class StatusPillRenderer extends javax.swing.table.DefaultTableCellRenderer {
        private Color pillBackground = HavenColors.SUCCESS_BG;
        private Color pillForeground = HavenColors.SUCCESS_TEXT;

        StatusPillRenderer() { setHorizontalAlignment(SwingConstants.CENTER); }

        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            String status = value == null ? "" : value.toString().trim();
            boolean confirmed = status.equalsIgnoreCase("CONFIRMED") || status.equalsIgnoreCase("Confirmed");
            setText(confirmed ? "Confirmed" : status);
            setFont(new Font("SansSerif", Font.BOLD, 10));
            setHorizontalAlignment(SwingConstants.CENTER);
            pillBackground = confirmed ? HavenColors.SUCCESS_BG : new Color(241, 245, 249);
            pillForeground = confirmed ? HavenColors.SUCCESS_TEXT : new Color(71, 85, 105);
            if (selected) {
                pillBackground = confirmed ? new Color(187, 247, 208) : new Color(226, 232, 240);
            }
            setForeground(pillForeground);
            setOpaque(false);
            return this;
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int pillWidth = Math.max(62, getWidth() - 18);
            int x = (getWidth() - pillWidth) / 2;
            g.setColor(pillBackground);
            g.fillRoundRect(x, 6, pillWidth, getHeight() - 12, 18, 18);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    /** Small custom Graphics2D amenities used inside each room portfolio card. */
    private static class AmenityVectorIcon implements Icon {
        private final String type;
        AmenityVectorIcon(String type) { this.type = type; }
        @Override public int getIconWidth() { return 15; }
        @Override public int getIconHeight() { return 15; }
        @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(HavenColors.DEEP_TEAL);
            g.setStroke(new BasicStroke(1.35f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (type) {
                case "wifi" -> { g.drawArc(1, 2, 13, 10, 35, 110); g.drawArc(4, 5, 7, 6, 35, 110); g.fillOval(7, 11, 2, 2); }
                case "ac" -> { g.drawRoundRect(1, 2, 13, 10, 2, 2); g.drawLine(4, 5, 11, 5); g.drawLine(5, 12, 4, 14); g.drawLine(10, 12, 11, 14); }
                case "breakfast" -> { g.drawOval(1, 5, 13, 8); g.drawLine(7, 2, 7, 5); g.drawLine(5, 2, 5, 4); g.drawLine(9, 2, 9, 4); }
                default -> { g.drawRoundRect(2, 2, 11, 12, 2, 2); g.drawLine(4, 5, 11, 5); g.drawLine(4, 8, 11, 8); }
            }
            g.dispose();
        }
    }

    /** Icon artwork for the three dashboard quick-action buttons. */
    private static class QuickActionVectorIcon implements Icon {
        private final String type;
        QuickActionVectorIcon(String type) { this.type = type; }
        @Override public int getIconWidth() { return 22; }
        @Override public int getIconHeight() { return 22; }
        @Override public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x, y);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(HavenColors.DEEP_TEAL);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (type.equals("booking")) {
                g.drawRoundRect(3, 3, 16, 16, 3, 3); g.drawLine(7, 1, 7, 6); g.drawLine(15, 1, 15, 6);
                g.drawLine(4, 8, 18, 8); g.drawLine(8, 13, 11, 16); g.drawLine(11, 16, 16, 11);
            } else if (type.equals("calendar")) {
                g.drawRoundRect(3, 3, 16, 16, 3, 3); g.drawLine(3, 8, 19, 8);
                g.drawLine(7, 1, 7, 5); g.drawLine(15, 1, 15, 5); g.drawOval(7, 11, 3, 3); g.drawOval(13, 11, 3, 3);
            } else {
                g.drawRoundRect(2, 5, 18, 13, 3, 3); g.drawLine(5, 9, 17, 9); g.drawLine(5, 13, 17, 13);
            }
            g.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { }
            new HotelReservationSystem().setVisible(true);
        });
    }
}
