import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import javax.swing.plaf.FontUIResource;

class xo {
    idenfier ident;
    size size;
}

enum idenfier {
    X, O; // X = หมาเทวดา (Angel Dogs), O = แมวปีศาจ (Devil Cats)

    idenfier other() {
        return this == X ? O : X;
    }
}

enum size {
    small, middle, large;
}

class Board {
    // แต่ละช่องเป็น stack ของหมาก (ตัวบนสุด = ตัวที่เห็น)
    Deque<xo>[][] board;

    @SuppressWarnings("unchecked")
    public Board() {
        this.board = new Deque[3][3];
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                board[r][c] = new ArrayDeque<>();
            }
        }
    }

    /** หมากตัวบนสุดของช่อง (null ถ้าช่องว่าง) */
    public xo top(int r, int c) {
        return board[r][c].peek();
    }

    /** วางได้ถ้าช่องว่าง หรือหมากที่จะวางใหญ่กว่าตัวบนสุด */
    public boolean canPlace(xo piece, int r, int c) {
        xo t = top(r, c);
        return t == null || piece.size.ordinal() > t.size.ordinal();
    }

    public void place(xo piece, int r, int c) {
        board[r][c].push(piece);
    }

    /** หยิบหมากตัวบนสุดออกจากช่อง */
    public xo lift(int r, int c) {
        return board[r][c].pop();
    }

    private idenfier at(int r, int c) {
        xo t = top(r, c);
        return t == null ? null : t.ident;
    }

    private boolean line(idenfier id, idenfier a, idenfier b, idenfier c) {
        return a == id && b == id && c == id;
    }

    /** ฝั่ง id เรียงตัวบนสุดครบ 3 ช่องหรือไม่ */
    public boolean hasWon(idenfier id) {
        for (int i = 0; i < 3; i++) {
            if (line(id, at(i, 0), at(i, 1), at(i, 2))) return true; // แนวนอน
            if (line(id, at(0, i), at(1, i), at(2, i))) return true; // แนวตั้ง
        }
        return line(id, at(0, 0), at(1, 1), at(2, 2))                // แนวทแยง
                || line(id, at(0, 2), at(1, 1), at(2, 0));
    }
}

// UI แบบ Console (ของเดิม ปรับให้แสดงตัวบนสุดและขนาด)
class UI {
    public void displayBoard(Board board) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                xo t = board.top(i, j);
                if (t == null) {
                    System.out.print("[  ]");
                } else {
                    System.out.print("[" + t.ident + (t.size.ordinal() + 1) + "]");
                }
            }
            System.out.println();
        }
    }
}

class player {
    int[] count = {2, 2, 2}; // small, middle, large ที่เหลือนอกกระดาน
    idenfier ident;
    Board board;

    public player(idenfier ident, Board board) {
        this.ident = ident;
        this.board = board;
    }

    private xo makePiece(size s) {
        xo piece = new xo();
        piece.ident = this.ident;
        piece.size = s;
        return piece;
    }

    /** วางหมากใหม่ได้ไหม (ยังเหลือ และวางลงช่องว่าง/ทับตัวที่เล็กกว่าได้) */
    public boolean canPlaceNew(size s, int row, int col) {
        if (count[s.ordinal()] == 0) return false;
        return board.canPlace(makePiece(s), row, col);
    }

    public boolean placeNew(size s, int row, int col) {
        if (!canPlaceNew(s, row, col)) return false;
        board.place(makePiece(s), row, col);
        count[s.ordinal()]--;
        return true;
    }

    /** ย้ายหมากตัวบนสุดของตัวเองจาก (fr,fc) ไป (tr,tc) ได้ไหม */
    public boolean canMove(int fr, int fc, int tr, int tc) {
        if (fr == tr && fc == tc) return false;
        xo piece = board.top(fr, fc);
        if (piece == null || piece.ident != ident) return false;
        return board.canPlace(piece, tr, tc);
    }

    /** มีตาเดินที่ทำได้อย่างน้อย 1 อย่างไหม */
    public boolean hasAnyMove() {
        for (size s : size.values()) {
            for (int r = 0; r < 3; r++)
                for (int c = 0; c < 3; c++)
                    if (canPlaceNew(s, r, c)) return true;
        }
        for (int fr = 0; fr < 3; fr++)
            for (int fc = 0; fc < 3; fc++)
                for (int tr = 0; tr < 3; tr++)
                    for (int tc = 0; tc < 3; tc++)
                        if (canMove(fr, fc, tr, tc)) return true;
        return false;
    }

    public boolean hasSupply() {
        for (int n : count) if (n > 0) return true;
        return false;
    }
}

/** เลือกฟอนต์ที่แสดงภาษาไทยได้ แล้วตั้งเป็นฟอนต์เริ่มต้นของทุกคอมโพเนนต์ */
class Fonts {
    static String family;
    private static boolean done;

    static void install() {
        if (done) return;
        done = true;
        family = pick();
        if (family == null) return;
        for (Object key : java.util.Collections.list(UIManager.getDefaults().keys())) {
            Object v = UIManager.get(key);
            if (v instanceof Font) {
                Font f = (Font) v;
                UIManager.put(key, new FontUIResource(new Font(family, f.getStyle(), f.getSize())));
            }
        }
    }

    static Font make(int style, int size) {
        return new Font(family != null ? family : "SansSerif", style, size);
    }

    private static String pick() {
        String test = "ตาเลือกขนาดวางหมากย้าย";
        Set<String> avail = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        String[] pref = {"Leelawadee UI", "Tahoma", "Segoe UI", "Noto Sans Thai", "Noto Sans Thai UI",
                "TH Sarabun New", "Angsana New", "Cordia New", "Browallia New", "Microsoft Sans Serif",
                "Arial Unicode MS", "Loma", "Garuda", "Norasi", "Waree", "Umpush"};
        for (String p : pref) {
            if (avail.contains(p) && new Font(p, Font.PLAIN, 14).canDisplayUpTo(test) == -1) return p;
        }
        for (String f : avail) {
            if (new Font(f, Font.PLAIN, 14).canDisplayUpTo(test) == -1) return f;
        }
        return null;
    }
}

/**
 * โหลดรูปทั้งหมดจากโฟลเดอร์ asset/ (หรือโฟลเดอร์เดียวกับที่รันเกม)
 *   Board.jpg                         -> กระดาน
 *   Doglv1.png Doglv2.png Doglv3.png  -> หมาเทวดา LV1 / LV2 / LV3  (X: small / middle / large)
 *   Catlv1.png Catlv2.png Catlv3.png  -> แมวปีศาจ LV1 / LV2 / LV3  (O: small / middle / large)
 * (รองรับนามสกุล .png .jpg .jpeg)
 */
class Assets {
    static final BufferedImage board = cleanBoard(load("Board"));
    static final BufferedImage[][] pieces = new BufferedImage[2][3];
    // ป้ายประกาศ: [ฝั่ง X=หมา / O=แมว][0 = ถึงตา, 1 = ชนะ]
    static final BufferedImage[][] banners = new BufferedImage[2][2];

    static {
        String[] prefix = {"Doglv", "Catlv"};
        for (int i = 0; i < 2; i++) {
            for (int s = 0; s < 3; s++) {
                BufferedImage img = load(prefix[i] + (s + 1));
                pieces[i][s] = img == null ? null : trimLabel(removeWhiteBackground(img));
            }
        }
    }

    /** โฟลเดอร์ที่จะลองหารูป (เทียบจากโฟลเดอร์ที่รัน และจากตำแหน่งไฟล์ .class) */
    private static File[] searchDirs() {
        java.util.List<File> dirs = new java.util.ArrayList<>();
        String[] names = {"asset", "assets", "Asset", "Assets", "."};
        java.util.List<File> roots = new java.util.ArrayList<>();
        roots.add(new File("."));
        try {
            File here = new File(Assets.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (here.isFile()) here = here.getParentFile();
            roots.add(here);
            if (here.getParentFile() != null) roots.add(here.getParentFile());
        } catch (Exception ignored) {
        }
        for (File root : roots)
            for (String n : names) dirs.add(new File(root, n));
        return dirs.toArray(new File[0]);
    }

    static {
        String[][] names = {{"DogTurn", "DogWIN"}, {"CatTurn", "CatWIN"}};
        for (int i = 0; i < 2; i++)
            for (int k = 0; k < 2; k++) {
                BufferedImage img = load(names[i][k]);
                banners[i][k] = img == null ? null : removeWhiteBackground(img);
            }
    }

    static BufferedImage banner(idenfier id, boolean win) {
        return banners[id.ordinal()][win ? 1 : 0];
    }

    static BufferedImage load(String base) {
        for (File dir : searchDirs()) {
            for (String ext : new String[]{".png", ".jpg", ".jpeg", ".PNG", ".JPG"}) {
                File f = new File(dir, base + ext);
                if (f.isFile()) {
                    try {
                        return ImageIO.read(f);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        System.err.println("หาไฟล์รูปไม่เจอ: " + base + " (ลองใน asset/ และโฟลเดอร์ที่รัน)");
        return null;
    }

    static BufferedImage piece(idenfier id, size s) {
        return pieces[id.ordinal()][s.ordinal()];
    }

    /** Board.jpg มีลายตารางหมากรุก (ขาว/เทา) แทนพื้นโปร่งใสติดมา -> ลบออกจากขอบรูปด้วย flood fill */
    static BufferedImage cleanBoard(BufferedImage src) {
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        boolean[] bg = new boolean[w * h];
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            stack.push(new int[]{x, 0});
            stack.push(new int[]{x, h - 1});
        }
        for (int y = 0; y < h; y++) {
            stack.push(new int[]{0, y});
            stack.push(new int[]{w - 1, y});
        }
        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            int x = p[0], y = p[1];
            if (x < 0 || y < 0 || x >= w || y >= h || bg[y * w + x]) continue;
            if (!isChecker(out.getRGB(x, y), 8)) continue;
            bg[y * w + x] = true;
            stack.push(new int[]{x + 1, y});
            stack.push(new int[]{x - 1, y});
            stack.push(new int[]{x, y + 1});
            stack.push(new int[]{x, y - 1});
        }
        // ขยายออกอีก 2 พิกเซล เก็บขอบที่เป็นสีเทาจาง ๆ จาก JPEG
        for (int pass = 0; pass < 2; pass++) {
            boolean[] next = bg.clone();
            for (int y = 1; y < h - 1; y++) {
                for (int x = 1; x < w - 1; x++) {
                    int i = y * w + x;
                    if (bg[i]) continue;
                    if ((bg[i - 1] || bg[i + 1] || bg[i - w] || bg[i + w]) && isChecker(out.getRGB(x, y), 40)) {
                        next[i] = true;
                    }
                }
            }
            bg = next;
        }
        for (int i = 0; i < bg.length; i++) {
            if (bg[i]) out.setRGB(i % w, i / w, 0x00000000);
        }
        return out;
    }

    /** สีเทากลาง (r≈g≈b) และสว่าง = พิกเซลของลายตาราง */
    private static boolean isChecker(int argb, int spread) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
        return mx - mn <= (spread > 10 ? 6 : 3) && mn >= (spread > 10 ? 170 : 190);
    }

    /** ลบพื้นหลังสีขาวที่ติดขอบรูป (flood fill จากขอบ ไม่ลบสีขาวบนตัวสัตว์) */
    static BufferedImage removeWhiteBackground(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        // ถ้ารูปมีพื้นหลังโปร่งใสอยู่แล้ว ไม่ต้องทำอะไร
        if (((out.getRGB(0, 0) >>> 24) & 0xFF) < 250) return out;

        boolean[] seen = new boolean[w * h];
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            stack.push(new int[]{x, 0});
            stack.push(new int[]{x, h - 1});
        }
        for (int y = 0; y < h; y++) {
            stack.push(new int[]{0, y});
            stack.push(new int[]{w - 1, y});
        }
        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            int x = p[0], y = p[1];
            if (x < 0 || y < 0 || x >= w || y >= h || seen[y * w + x]) continue;
            seen[y * w + x] = true;
            int argb = out.getRGB(x, y);
            int r = (argb >> 16) & 0xFF, gg = (argb >> 8) & 0xFF, b = argb & 0xFF;
            if (r > 235 && gg > 235 && b > 235) {
                out.setRGB(x, y, 0x00FFFFFF);
                stack.push(new int[]{x + 1, y});
                stack.push(new int[]{x - 1, y});
                stack.push(new int[]{x, y + 1});
                stack.push(new int[]{x, y - 1});
            }
        }
        return out;
    }

    /** รูปหมา/แมวมีป้ายข้อความ (เช่น "ANGEL DOGS: LV1") อยู่ใต้ตัว -> ตัดทิ้ง เหลือเฉพาะตัวสัตว์ */
    static BufferedImage trimLabel(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        int first = -1, last = -1, gap = 0;
        for (int y = 0; y < h; y++) {
            boolean ink = false;
            for (int x = 0; x < w && !ink; x++) ink = ((src.getRGB(x, y) >>> 24) & 0xFF) > 20;
            if (ink) {
                if (first < 0) first = y;
                else if (gap >= 7) break; // ช่องว่างกว้าง = จบตัวสัตว์ ต่อจากนี้คือข้อความ
                last = y;
                gap = 0;
            } else if (first >= 0) {
                gap++;
            }
        }
        if (first < 0) return src;
        int minX = w, maxX = -1;
        for (int y = first; y <= last; y++) {
            for (int x = 0; x < w; x++) {
                if (((src.getRGB(x, y) >>> 24) & 0xFF) > 20) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                }
            }
        }
        BufferedImage out = new BufferedImage(maxX - minX + 1, last - first + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, -minX, -first, null);
        g.dispose();
        return out;
    }

    /** ย่อรูปคุณภาพดี (ย่อทีละครึ่ง) */
    static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage cur = src;
        int cw = src.getWidth(), ch = src.getHeight();
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2;
            ch /= 2;
            cur = step(cur, cw, ch);
        }
        return step(cur, Math.max(1, w), Math.max(1, h));
    }

    private static BufferedImage step(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    static ImageIcon icon(idenfier id, size s, int height) {
        BufferedImage img = piece(id, s);
        if (img == null) return null;
        int w = Math.max(1, img.getWidth() * height / img.getHeight());
        return new ImageIcon(scale(img, w, height));
    }
}

// UI แบบกราฟิก (Swing) ใช้รูป Board.jpg เป็นกระดาน และรูปหมา/แมวเป็นหมาก
class GameGUI {
    // ตำแหน่งกึ่งกลางของแต่ละช่องบนรูป Board.jpg (เป็นสัดส่วน 0..1 ของความกว้าง/สูงรูป)
    // ถ้าวางหมากไม่ตรงช่อง ให้ปรับตัวเลขตรงนี้
    private static final double[][] CX = {
            {0.413, 0.521, 0.637},
            {0.406, 0.519, 0.640},
            {0.400, 0.523, 0.647}
    };
    private static final double[][] CY = {
            {0.290, 0.290, 0.290},
            {0.442, 0.442, 0.442},
            {0.607, 0.607, 0.607}
    };
    private static final double HIT_W = 0.060;  // ครึ่งความกว้างช่อง (สำหรับคลิก)
    private static final double HIT_H = 0.080;  // ครึ่งความสูงช่อง
    // กล่องสูงสุดของหมากแต่ละขนาด (small/middle/large) เป็นสัดส่วนของความกว้าง/สูงรูปกระดาน
    private static final double[] BOX_W = {0.058, 0.082, 0.118};
    private static final double[] BOX_H = {0.115, 0.150, 0.185};

    private Board board;
    private player playerX;
    private player playerO;
    private player current;
    private size selectedSize;
    private boolean placeMode;   // true = วางหมากใหม่, false = ย้ายหมาก
    private int fromR, fromC;    // ช่องที่เลือกไว้ตอนย้าย (-1 = ยังไม่เลือก)
    private boolean gameOver;

    // ป้ายประกาศที่แสดงกลางกระดาน (ถึงตา / ชนะ)
    private static final int BANNER_MS = 1500;   // เวลาที่ป้ายแสดง
    private static final int BANNER_FADE_IN = 150;
    private static final int BANNER_FADE_OUT = 300;
    private BufferedImage bannerImg;
    private long bannerStart;
    private javax.swing.Timer bannerTimer;

    private final JFrame frame = new JFrame("GobblePets");
    private final BoardPanel boardPanel = new BoardPanel();
    private final JToggleButton[] sizeButtons = new JToggleButton[3];
    private final JToggleButton placeBtn = new JToggleButton("วางหมากใหม่");
    private final JToggleButton moveBtn = new JToggleButton("ย้ายหมาก");
    private final JLabel status = new JLabel("", SwingConstants.CENTER);

    public GameGUI() {
        Fonts.install();
        buildUI();
        newGame();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static String nameOf(idenfier id) {
        return id == idenfier.X ? "หมาเทวดา" : "แมวปีศาจ";
    }

    private static String sizeName(size s) {
        switch (s) {
            case small:  return "LV1 เล็ก";
            case middle: return "LV2 กลาง";
            default:     return "LV3 ใหญ่";
        }
    }

    // ---------------------------------------------------------------- วาดกระดาน
    private class BoardPanel extends JPanel {
        private BufferedImage cache;
        private int cacheW, cacheH;

        BoardPanel() {
            setOpaque(true);
            setBackground(new Color(0xDFF0C8));
            ToolTipManager.sharedInstance().registerComponent(this);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int[] cell = cellAt(e.getX(), e.getY());
                    if (cell != null) onCellClicked(cell[0], cell[1]);
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            if (Assets.board == null) return new Dimension(900, 487);
            int w = 900;
            return new Dimension(w, w * Assets.board.getHeight() / Assets.board.getWidth());
        }

        /** พื้นที่ที่รูปกระดานถูกวาด (รักษาอัตราส่วน) */
        private Rectangle fit() {
            int pw = getWidth(), ph = getHeight();
            double ar = Assets.board == null ? 900.0 / 487 : (double) Assets.board.getWidth() / Assets.board.getHeight();
            int w = pw, h = (int) (pw / ar);
            if (h > ph) {
                h = ph;
                w = (int) (ph * ar);
            }
            return new Rectangle((pw - w) / 2, (ph - h) / 2, w, h);
        }

        private int[] cellAt(int x, int y) {
            Rectangle r = fit();
            double fx = (x - r.x) / (double) r.width;
            double fy = (y - r.y) / (double) r.height;
            int[] best = null;
            double bestD = Double.MAX_VALUE;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    double dx = Math.abs(fx - CX[i][j]), dy = Math.abs(fy - CY[i][j]);
                    if (dx <= HIT_W && dy <= HIT_H && dx * dx + dy * dy < bestD) {
                        bestD = dx * dx + dy * dy;
                        best = new int[]{i, j};
                    }
                }
            }
            return best;
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            int[] cell = cellAt(e.getX(), e.getY());
            if (cell == null || board == null || board.top(cell[0], cell[1]) == null) return null;
            StringBuilder sb = new StringBuilder("บนสุด→ล่างสุด: ");
            for (xo q : board.board[cell[0]][cell[1]]) {
                sb.append(nameOf(q.ident)).append('(').append(sizeName(q.size)).append(") ");
            }
            return sb.toString();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            Rectangle r = fit();
            if (Assets.board != null) {
                if (cache == null || cacheW != r.width || cacheH != r.height) {
                    cache = Assets.scale(Assets.board, r.width, r.height);
                    cacheW = r.width;
                    cacheH = r.height;
                }
                g.drawImage(cache, r.x, r.y, null);
            } else {
                g.setColor(new Color(0xC8A56A));
                g.fillRect(r.x, r.y, r.width, r.height);
                g.setColor(Color.DARK_GRAY);
                g.drawString("ไม่พบไฟล์ Board.jpg", r.x + 20, r.y + 30);
            }

            if (board == null) {
                g.dispose();
                return;
            }

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    int cx = r.x + (int) (CX[i][j] * r.width);
                    int cy = r.y + (int) (CY[i][j] * r.height);

                    // ไฮไลต์ช่องที่เลือกไว้เพื่อย้าย
                    if (i == fromR && j == fromC) {
                        int hw = (int) (HIT_W * 2 * r.width * 0.92);
                        int hh = (int) (HIT_H * 2 * r.height * 0.92);
                        g.setColor(new Color(255, 190, 40, 90));
                        g.fillRoundRect(cx - hw / 2, cy - hh / 2, hw, hh, 24, 24);
                        g.setStroke(new BasicStroke(4f));
                        g.setColor(new Color(255, 140, 0));
                        g.drawRoundRect(cx - hw / 2, cy - hh / 2, hw, hh, 24, 24);
                    }

                    xo p = board.top(i, j);
                    if (p == null) continue;
                    drawPiece(g, p, cx, cy, r);
                }
            }
            drawBanner(g, r);
            g.dispose();
        }

        private BufferedImage bannerScaled;
        private BufferedImage bannerScaledSrc;
        private int bannerScaledW;

        /** วาดป้ายประกาศตรงกลางกระดาน (เฟดเข้า/ออก) */
        private void drawBanner(Graphics2D g, Rectangle r) {
            BufferedImage img = bannerImg;
            if (img == null) return;
            long t = System.currentTimeMillis() - bannerStart;
            if (t < 0 || t >= BANNER_MS) return;

            float alpha = 1f;
            if (t < BANNER_FADE_IN) alpha = t / (float) BANNER_FADE_IN;
            else if (t > BANNER_MS - BANNER_FADE_OUT) alpha = (BANNER_MS - t) / (float) BANNER_FADE_OUT;
            alpha = Math.max(0f, Math.min(1f, alpha));

            int w = (int) (r.width * 0.50);
            int h = (int) ((double) img.getHeight() * w / img.getWidth());
            if (h > r.height * 0.6) { // กันป้ายสูงเกินกระดาน
                h = (int) (r.height * 0.6);
                w = (int) ((double) img.getWidth() * h / img.getHeight());
            }
            if (bannerScaled == null || bannerScaledSrc != img || bannerScaledW != w) {
                bannerScaled = Assets.scale(img, w, h);
                bannerScaledSrc = img;
                bannerScaledW = w;
            }
            // เล่นให้ป้ายขยายขึ้นเล็กน้อยตอนเฟดเข้า
            double pop = t < BANNER_FADE_IN ? 0.92 + 0.08 * (t / (double) BANNER_FADE_IN) : 1.0;
            int dw = (int) (bannerScaled.getWidth() * pop), dh = (int) (bannerScaled.getHeight() * pop);
            int x = r.x + (r.width - dw) / 2, y = r.y + (r.height - dh) / 2;

            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive(alpha));
            g.drawImage(bannerScaled, x, y, dw, dh, null);
            g.setComposite(old);
        }

        private void drawPiece(Graphics2D g, xo p, int cx, int cy, Rectangle r) {
            BufferedImage img = Assets.piece(p.ident, p.size);
            int k = p.size.ordinal();
            int boxW = (int) (BOX_W[k] * r.width), boxH = (int) (BOX_H[k] * r.height);
            if (img == null) { // ไม่มีรูป -> ใช้ตัวอักษรแทน
                g.setColor(p.ident == idenfier.X ? new Color(30, 90, 200) : new Color(200, 40, 40));
                g.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, boxH)));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(p.ident.toString(), cx - fm.stringWidth(p.ident.toString()) / 2, cy + fm.getAscent() / 3);
                return;
            }
            double sc = Math.min(boxW / (double) img.getWidth(), boxH / (double) img.getHeight());
            int w = Math.max(1, (int) (img.getWidth() * sc)), h = Math.max(1, (int) (img.getHeight() * sc));
            BufferedImage scaled = Assets.scale(img, w, h);
            int footY = cy + boxH / 2 - boxH / 10; // จุดที่ตัวหมากยืนบนช่อง
            // เงาใต้ตัวหมาก
            g.setColor(new Color(0, 0, 0, 55));
            g.fillOval(cx - w / 3, footY - h / 14, w * 2 / 3, Math.max(4, h / 7));
            g.drawImage(scaled, cx - w / 2, footY - h, null);
        }
    }

    // ---------------------------------------------------------------- ประกอบหน้าจอ
    private void buildUI() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(0, 0));

        // ด้านบน: สถานะ
        status.setFont(Fonts.make(Font.BOLD, 16));
        status.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
        frame.add(status, BorderLayout.NORTH);

        // กลาง: รูปกระดาน
        frame.add(boardPanel, BorderLayout.CENTER);

        // ล่าง: โหมด + เลือกขนาด + เริ่มใหม่
        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));

        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(placeBtn);
        modeGroup.add(moveBtn);
        placeBtn.addActionListener(e -> setMode(true));
        moveBtn.addActionListener(e -> setMode(false));
        modeRow.add(placeBtn);
        modeRow.add(moveBtn);

        JPanel sizeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        ButtonGroup sizeGroup = new ButtonGroup();
        size[] all = size.values();
        for (int i = 0; i < all.length; i++) {
            final size s = all[i];
            JToggleButton tb = new JToggleButton(sizeName(s));
            tb.setHorizontalTextPosition(SwingConstants.CENTER);
            tb.setVerticalTextPosition(SwingConstants.BOTTOM);
            tb.addActionListener(e -> selectedSize = s);
            sizeButtons[i] = tb;
            sizeGroup.add(tb);
            sizeRow.add(tb);
        }
        JButton reset = new JButton("New Game");
        reset.addActionListener(e -> newGame());
        sizeRow.add(reset);

        bottom.add(modeRow);
        bottom.add(sizeRow);
        frame.add(bottom, BorderLayout.SOUTH);
    }

    /** แสดงป้ายประกาศกลางกระดาน 1.5 วินาที (win=true ป้ายชนะ, false ป้ายถึงตา) */
    private void showBanner(idenfier id, boolean win) {
        BufferedImage img = Assets.banner(id, win);
        if (img == null) return;
        bannerImg = img;
        bannerStart = System.currentTimeMillis();
        if (bannerTimer != null) bannerTimer.stop();
        bannerTimer = new javax.swing.Timer(25, e -> {
            if (System.currentTimeMillis() - bannerStart >= BANNER_MS) {
                bannerImg = null;
                bannerTimer.stop();
            }
            boardPanel.repaint();
        });
        bannerTimer.start();
        boardPanel.repaint();
    }

    private void newGame() {
        board = new Board();
        playerX = new player(idenfier.X, board);
        playerO = new player(idenfier.O, board);
        current = playerX;
        selectedSize = size.small;
        sizeButtons[0].setSelected(true);
        placeMode = true;
        placeBtn.setSelected(true);
        fromR = -1;
        fromC = -1;
        gameOver = false;
        refresh();
        status.setText("ตา: " + nameOf(current.ident) + " — เลือกขนาดแล้วคลิกช่องเพื่อวางหมาก");
        showBanner(current.ident, false);
    }

    private void setMode(boolean place) {
        placeMode = place;
        fromR = -1;
        fromC = -1;
        refresh();
        status.setText("ตา: " + nameOf(current.ident) + (place
                ? " — เลือกขนาดแล้วคลิกช่องเพื่อวางหมาก"
                : " — คลิกหมากของคุณที่อยู่บนสุดเพื่อย้าย"));
    }

    private void onCellClicked(int r, int c) {
        if (gameOver) return;

        if (placeMode) {
            if (!current.placeNew(selectedSize, r, c)) {
                status.setText("วางไม่ได้! ต้องวางในช่องว่าง หรือทับหมากที่เล็กกว่าเท่านั้น");
                return;
            }
            if (board.hasWon(current.ident)) {
                finish(nameOf(current.ident) + " เรียงครบ 3 ตัว ชนะ!", current.ident);
                return;
            }
            nextTurn();
            return;
        }

        // โหมดย้ายหมาก
        if (fromR < 0) {
            xo t = board.top(r, c);
            if (t == null || t.ident != current.ident) {
                status.setText("ต้องเลือกหมากของคุณที่อยู่บนสุดของช่อง");
                return;
            }
            fromR = r;
            fromC = c;
            refresh();
            status.setText("เลือกช่องปลายทาง (คลิกช่องเดิมซ้ำเพื่อยกเลิก)");
            return;
        }

        if (r == fromR && c == fromC) {
            fromR = -1;
            fromC = -1;
            refresh();
            status.setText("ตา: " + nameOf(current.ident) + " — คลิกหมากของคุณที่อยู่บนสุดเพื่อย้าย");
            return;
        }
        if (!current.canMove(fromR, fromC, r, c)) {
            status.setText("ย้ายไปช่องนี้ไม่ได้! ต้องเป็นช่องว่าง หรือทับหมากที่เล็กกว่า");
            return;
        }
        doMove(fromR, fromC, r, c);
    }

    private void doMove(int fr, int fc, int tr, int tc) {
        // 1) ยกหมากออก -> ถ้าตัวด้านล่างที่เผยออกมาทำให้ฝั่งตรงข้ามเรียงครบ 3 ฝั่งตรงข้ามชนะทันที
        xo piece = board.lift(fr, fc);
        idenfier opp = current.ident.other();
        if (board.hasWon(opp)) {
            fromR = -1;
            fromC = -1;
            finish(nameOf(current.ident) + " ยกหมากออก ทำให้ " + nameOf(opp) + " เรียงครบ 3 ตัว " + nameOf(opp) + " ชนะ!", opp);
            return;
        }

        // 2) วางที่ช่องใหม่ แล้วเช็กว่าเราชนะหรือไม่
        board.place(piece, tr, tc);
        if (board.hasWon(current.ident)) {
            fromR = -1;
            fromC = -1;
            finish(nameOf(current.ident) + " เรียงครบ 3 ตัว ชนะ!", current.ident);
            return;
        }
        nextTurn();
    }

    private void finish(String message, idenfier winner) {
        gameOver = true;
        refresh();
        status.setText(message);
        showBanner(winner, true);
    }

    private void nextTurn() {
        current = (current == playerX) ? playerO : playerX;
        fromR = -1;
        fromC = -1;

        if (!current.hasAnyMove()) {
            idenfier skipped = current.ident;
            current = (current == playerX) ? playerO : playerX;
            if (!current.hasAnyMove()) {
                gameOver = true;
                refresh();
                status.setText("เสมอ! ไม่มีใครเดินได้แล้ว");
                return;
            }
            refresh();
            status.setText(nameOf(skipped) + " ไม่มีตาเดินที่ทำได้ ข้ามตา — ตา: " + nameOf(current.ident));
            showBanner(current.ident, false);
            return;
        }

        refresh();
        status.setText("ตา: " + nameOf(current.ident));
        showBanner(current.ident, false);
    }

    /** อัปเดตปุ่มและวาดกระดานใหม่ตามสถานะปัจจุบัน */
    private void refresh() {
        // ถ้าไม่เหลือหมากนอกกระดานแล้ว ต้องใช้โหมดย้ายเท่านั้น
        if (!current.hasSupply() && placeMode) {
            placeMode = false;
            moveBtn.setSelected(true);
        }

        placeBtn.setEnabled(current.hasSupply() && !gameOver);
        moveBtn.setEnabled(!gameOver);

        // ปุ่มเลือกขนาด: แสดงรูปหมาก/แมวของผู้เล่นปัจจุบัน + จำนวนที่เหลือ
        size[] all = size.values();
        for (int i = 0; i < all.length; i++) {
            int left = current.count[i];
            sizeButtons[i].setIcon(Assets.icon(current.ident, all[i], 56));
            sizeButtons[i].setText(sizeName(all[i]) + " (" + left + ")");
            sizeButtons[i].setEnabled(placeMode && left > 0 && !gameOver);
        }
        // ถ้าขนาดที่เลือกอยู่หมดแล้ว ให้เลื่อนไปขนาดที่ยังเหลือ
        if (current.count[selectedSize.ordinal()] == 0) {
            for (int i = 0; i < all.length; i++) {
                if (current.count[i] > 0) {
                    selectedSize = all[i];
                    sizeButtons[i].setSelected(true);
                    break;
                }
            }
        }
        sizeButtons[selectedSize.ordinal()].setSelected(true);

        boardPanel.repaint();
    }
}

// main class สำหรับรันเกม
class Xogame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Fonts.install();
            new GameGUI();
        });
    }
}