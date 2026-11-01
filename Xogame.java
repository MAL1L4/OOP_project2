import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * เกม XO แบบซ้อนทับ (Stacking)
 *
 * วิธีเล่น
 * 1. ผู้เล่นแต่ละฝั่งมีหมาก 6 ตัว: ใหญ่ 2, กลาง 2, เล็ก 2
 * 2. แต่ละเทิร์นเลือกทำอย่างใดอย่างหนึ่ง
 *    a. วางหมากใหม่จากมือ ลงช่องว่าง หรือทับหมากที่เล็กกว่า
 *    b. ย้ายหมากของตัวเองที่อยู่บนสุดของช่อง ไปช่องว่าง หรือทับหมากที่เล็กกว่า
 * 3. ถ้ายกหมากของตัวเองออกแล้วเผยให้ฝั่งตรงข้ามเรียงครบ 3 ตัว ฝั่งตรงข้ามชนะทันที
 * 4. ผู้ที่เรียงหมาก "ชั้นบนสุด" ของตัวเองครบ 3 ช่องติดกัน (แนวตั้ง/นอน/ทแยง) ก่อนเป็นผู้ชนะ
 */
public class Xogame extends JFrame {

    private static final int EMPTY = -1;
    private static final String[] PLAYER = {"X", "O"};
    private static final Color[] COLOR = {new Color(220, 50, 50), new Color(40, 90, 220)};
    private static final String[] SIZE_NAME = {"เล็ก", "กลาง", "ใหญ่"};
    private static final double[] SIZE_RATIO = {0.40, 0.65, 0.90};
    private static final int PIECES_PER_SIZE = 2; // จำนวนหมากต่อขนาด

    private static final Color WIN_COLOR = new Color(255, 235, 130);

    // เส้นชนะทั้ง 8 เส้น: {r1,c1, r2,c2, r3,c3}
    private static final int[][] LINES = {
        {0, 0, 0, 1, 0, 2}, {1, 0, 1, 1, 1, 2}, {2, 0, 2, 1, 2, 2},
        {0, 0, 1, 0, 2, 0}, {0, 1, 1, 1, 2, 1}, {0, 2, 1, 2, 2, 2},
        {0, 0, 1, 1, 2, 2}, {0, 2, 1, 1, 2, 0}
    };

    // หมากหนึ่งตัว
    private static class Piece {
        final int owner; // 0 = X, 1 = O
        final int size;  // 0 = เล็ก, 1 = กลาง, 2 = ใหญ่
        Piece(int owner, int size) {
            this.owner = owner;
            this.size = size;
        }
    }

    private final CellPanel[][] cells = new CellPanel[3][3];
    private final JButton[][] handBtn = new JButton[2][3];
    private final int[][] hand = new int[2][3]; // hand[ผู้เล่น][ขนาด] = จำนวนที่เหลือ
    private final JLabel status = new JLabel("", SwingConstants.CENTER);
    private final JLabel hint = new JLabel("", SwingConstants.CENTER);

    private int current = 0;
    private boolean gameOver = false;

    // สถานะการเลือก: เลือกจากมือ (selectedSize) หรือเลือกหมากบนกระดาน (fromR, fromC)
    private int selectedSize = -1;
    private int fromR = -1, fromC = -1;
    private boolean selRevealsOpp = false; // ยกหมากที่เลือกแล้วจะเผยให้ฝั่งตรงข้ามเรียงครบ 3 หรือไม่

    public Xogame() {
        setTitle("เกม XO แบบซ้อนทับ");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        status.setFont(new Font("Tahoma", Font.BOLD, 20));
        hint.setFont(new Font("Tahoma", Font.PLAIN, 14));
        JPanel north = new JPanel(new GridLayout(2, 1, 0, 2));
        north.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        north.add(status);
        north.add(hint);
        add(north, BorderLayout.NORTH);

        JPanel board = new JPanel(new GridLayout(3, 3, 5, 5));
        board.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                cells[r][c] = new CellPanel(r, c);
                board.add(cells[r][c]);
            }
        }
        add(board, BorderLayout.CENTER);
        add(buildHandPanel(0), BorderLayout.WEST);
        add(buildHandPanel(1), BorderLayout.EAST);

        JPanel south = new JPanel(new BorderLayout(0, 5));
        JLabel rule = new JLabel("วางหมากใหม่ หรือย้ายหมากของตัวเองที่อยู่บนสุด | หมากใหญ่ทับหมากเล็กกว่าได้",
                SwingConstants.CENTER);
        rule.setFont(new Font("Tahoma", Font.PLAIN, 13));
        JButton reset = new JButton("เริ่มใหม่");
        reset.setFont(new Font("Tahoma", Font.PLAIN, 16));
        reset.addActionListener(e -> resetGame());
        south.add(rule, BorderLayout.NORTH);
        south.add(reset, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        resetGame();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ---------- ส่วนประกอบหน้าจอ ----------

    private JPanel buildHandPanel(int p) {
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("ผู้เล่น " + PLAYER[p], SwingConstants.CENTER);
        title.setFont(new Font("Tahoma", Font.BOLD, 18));
        title.setForeground(COLOR[p]);
        panel.add(title);

        for (int s = 2; s >= 0; s--) {
            JButton b = new JButton(new PieceIcon(p, s));
            b.setFocusPainted(false);
            final int size = s;
            b.addActionListener(e -> {
                if (gameOver || p != current) return;
                clearSelection();
                selectedSize = size; // วางหมากใหม่จากมือ
                refreshHands();
                updateStatus();
            });
            handBtn[p][s] = b;
            panel.add(b);
        }
        return panel;
    }

    // ช่องบนกระดาน: เก็บ "กอง" หมากที่ซ้อนกันอยู่
    private class CellPanel extends JPanel {
        final List<Piece> stack = new ArrayList<>();

        CellPanel(int r, int c) {
            setPreferredSize(new Dimension(120, 120));
            setOpaque(true);
            setBackground(Color.WHITE);
            setSelected(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    clickCell(r, c);
                }
            });
        }

        void setSelected(boolean sel) {
            setBorder(sel ? BorderFactory.createLineBorder(new Color(0, 160, 80), 5)
                          : BorderFactory.createLineBorder(Color.GRAY, 2));
        }

        Piece top() {
            return stack.isEmpty() ? null : stack.get(stack.size() - 1);
        }

        boolean canPlace(int size) {
            Piece t = top();
            return t == null || t.size < size;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), box = Math.min(w, h);

            Piece t = top();
            if (t != null) {
                drawPiece(g, t, w / 2, h / 2, box);
                // จุดเล็ก ๆ มุมซ้ายล่าง แสดงว่าใต้หมากบนสุดมีหมากของใครซ่อนอยู่
                int d = box / 10;
                for (int i = 0; i < stack.size() - 1; i++) {
                    g.setColor(COLOR[stack.get(i).owner]);
                    g.fillOval(8 + i * (d + 3), h - d - 8, d, d);
                }
            }
            g.dispose();
        }
    }

    // ไอคอนรูปหมากบนปุ่มเลือกขนาด
    private static class PieceIcon implements Icon {
        private final Piece piece;

        PieceIcon(int owner, int size) {
            piece = new Piece(owner, size);
        }

        @Override
        public int getIconWidth() {
            return 44;
        }

        @Override
        public int getIconHeight() {
            return 44;
        }

        @Override
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawPiece(g, piece, x + 22, y + 22, 44);
            g.dispose();
        }
    }

    private static void drawPiece(Graphics2D g, Piece p, int cx, int cy, int box) {
        int d = (int) (box * SIZE_RATIO[p.size]);
        g.setColor(COLOR[p.owner]);
        g.fillOval(cx - d / 2, cy - d / 2, d, d);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, Math.max(10, d / 2)));
        FontMetrics fm = g.getFontMetrics();
        String s = PLAYER[p.owner];
        g.drawString(s, cx - fm.stringWidth(s) / 2, cy + (fm.getAscent() - fm.getDescent()) / 2);
    }

    // ---------- การคลิกช่อง ----------

    private void clickCell(int r, int c) {
        if (gameOver) return;
        CellPanel cell = cells[r][c];
        Piece t = cell.top();

        // โหมดย้ายหมากบนกระดาน (เลือกหมากไว้แล้ว)
        if (fromR >= 0) {
            if (r == fromR && c == fromC) { // คลิกช่องเดิม = ยกเลิก
                clearSelection();
                refreshHands();
                updateStatus();
                return;
            }
            Piece moving = cells[fromR][fromC].top();
            if (cell.canPlace(moving.size)) {
                moveOnBoard(r, c);
            } else if (t != null && t.owner == current) {
                selectBoardPiece(r, c); // เปลี่ยนไปเลือกหมากตัวอื่นของตัวเอง
            } else {
                hint.setText("ย้ายไม่ได้! ต้องเป็นช่องว่าง หรือช่องที่มีหมากเล็กกว่า");
            }
            return;
        }

        // โหมดวางหมากใหม่จากมือ (เลือกขนาดไว้แล้ว)
        if (selectedSize >= 0) {
            if (cell.canPlace(selectedSize)) {
                placeFromHand(r, c);
            } else if (t != null && t.owner == current) {
                selectBoardPiece(r, c); // ช่องนี้วางทับไม่ได้ แต่เป็นหมากของเรา = เลือกเพื่อย้าย
            } else {
                hint.setText("วางไม่ได้! ต้องใช้หมากที่ใหญ่กว่าหมากในช่องนั้น");
            }
            return;
        }

        // ยังไม่ได้เลือกอะไร
        if (t != null && t.owner == current) {
            selectBoardPiece(r, c);
        } else {
            hint.setText("กรุณาเลือกหมากจากมือ หรือคลิกหมากของตัวเองบนกระดาน");
        }
    }

    private void selectBoardPiece(int r, int c) {
        clearSelection();
        fromR = r;
        fromC = c;

        // ลองยกหมากออก เพื่อดูว่าจะเผยให้ฝั่งตรงข้ามเรียงครบ 3 หรือไม่
        CellPanel cell = cells[r][c];
        Piece p = cell.stack.remove(cell.stack.size() - 1);
        selRevealsOpp = findLine(1 - current) != null;
        cell.stack.add(p);

        cell.setSelected(true);
        refreshHands();
        updateStatus();
    }

    private void clearSelection() {
        selectedSize = -1;
        fromR = -1;
        fromC = -1;
        selRevealsOpp = false;
        for (CellPanel[] row : cells) {
            for (CellPanel cell : row) cell.setSelected(false);
        }
    }

    // ---------- การเดินหมาก ----------

    private void placeFromHand(int r, int c) {
        cells[r][c].stack.add(new Piece(current, selectedSize));
        hand[current][selectedSize]--;
        clearSelection();
        cells[r][c].repaint();
        endMove(null);
    }

    private void moveOnBoard(int r, int c) {
        CellPanel from = cells[fromR][fromC];
        Piece p = from.stack.remove(from.stack.size() - 1);

        // ตรวจหลังยกหมากออก: ฝั่งตรงข้ามเรียงครบ 3 หรือไม่ (ถ้าใช่ ฝั่งตรงข้ามชนะทันที)
        int[] oppLine = findLine(1 - current);

        cells[r][c].stack.add(p);
        clearSelection();
        from.repaint();
        cells[r][c].repaint();
        endMove(oppLine);
    }

    // จบการเดิน: ตัดสินผลแพ้ชนะ หรือสลับตา
    private void endMove(int[] oppLine) {
        hint.setText("");
        if (oppLine != null) {
            highlight(oppLine);
            status.setText(PLAYER[1 - current] + " ชนะ!");
            hint.setText(PLAYER[current] + " ยกหมากแล้วเปิดให้ " + PLAYER[1 - current] + " เรียงครบ 3 ตัว");
            gameOver = true;
        } else {
            int[] line = findLine(current);
            if (line != null) {
                highlight(line);
                status.setText(PLAYER[current] + " ชนะ!");
                gameOver = true;
            } else {
                current = 1 - current;
                if (!hasAnyMove(current)) {
                    status.setText("เสมอ!");
                    hint.setText(PLAYER[current] + " ไม่มีทางเดินต่อ");
                    gameOver = true;
                } else {
                    updateStatus();
                }
            }
        }
        refreshHands();
    }

    // ---------- ตรรกะตรวจสอบ ----------

    // เจ้าของหมากชั้นบนสุดของช่อง (EMPTY ถ้าว่าง)
    private int topOwner(int r, int c) {
        Piece t = cells[r][c].top();
        return t == null ? EMPTY : t.owner;
    }

    // หาเส้นที่ผู้เล่น owner เรียงหมากชั้นบนสุดครบ 3 ช่อง (ไม่มี = null)
    private int[] findLine(int owner) {
        for (int[] L : LINES) {
            if (topOwner(L[0], L[1]) == owner
                    && topOwner(L[2], L[3]) == owner
                    && topOwner(L[4], L[5]) == owner) {
                return L;
            }
        }
        return null;
    }

    // ผู้เล่น p มีทางเดินอย่างน้อย 1 ทางหรือไม่ (วางจากมือ หรือย้ายหมากบนกระดาน)
    private boolean hasAnyMove(int p) {
        for (int s = 0; s < 3; s++) {
            if (hand[p][s] == 0) continue;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (cells[r][c].canPlace(s)) return true;
                }
            }
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                Piece t = cells[r][c].top();
                if (t == null || t.owner != p) continue;
                for (int r2 = 0; r2 < 3; r2++) {
                    for (int c2 = 0; c2 < 3; c2++) {
                        if ((r2 != r || c2 != c) && cells[r2][c2].canPlace(t.size)) return true;
                    }
                }
            }
        }
        return false;
    }

    private void highlight(int[] line) {
        for (int i = 0; i < 6; i += 2) {
            cells[line[i]][line[i + 1]].setBackground(WIN_COLOR);
        }
    }

    // ---------- อัปเดตหน้าจอ ----------

    private void updateStatus() {
        if (gameOver) return;
        status.setText("ตาของ " + PLAYER[current]);
        if (fromR >= 0) {
            if (selRevealsOpp) {
                hint.setText("ระวัง! ย้ายหมากตัวนี้แล้ว " + PLAYER[1 - current]
                        + " จะชนะทันที (คลิกช่องเดิมเพื่อยกเลิก)");
            } else {
                int size = cells[fromR][fromC].top().size;
                hint.setText("ย้ายหมาก" + SIZE_NAME[size] + ": คลิกช่องปลายทาง (คลิกช่องเดิมเพื่อยกเลิก)");
            }
        } else if (selectedSize >= 0) {
            hint.setText("วางหมาก" + SIZE_NAME[selectedSize] + ": คลิกช่องที่ต้องการ");
        } else {
            hint.setText("เลือกหมากจากมือ หรือคลิกหมากของตัวเองบนกระดานเพื่อย้าย");
        }
    }

    private void refreshHands() {
        for (int p = 0; p < 2; p++) {
            for (int s = 0; s < 3; s++) {
                boolean mine = (p == current) && !gameOver;
                boolean selected = mine && fromR < 0 && s == selectedSize;
                JButton b = handBtn[p][s];
                b.setText((selected ? "▶ " : "") + SIZE_NAME[s] + " ×" + hand[p][s]);
                b.setFont(new Font("Tahoma", selected ? Font.BOLD : Font.PLAIN, 15));
                b.setEnabled(mine && hand[p][s] > 0);
            }
        }
    }

    private void resetGame() {
        for (CellPanel[] row : cells) {
            for (CellPanel cell : row) {
                cell.stack.clear();
                cell.setBackground(Color.WHITE);
                cell.repaint();
            }
        }
        for (int p = 0; p < 2; p++) {
            for (int s = 0; s < 3; s++) hand[p][s] = PIECES_PER_SIZE;
        }
        current = 0;
        gameOver = false;
        clearSelection();
        refreshHands();
        updateStatus();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Xogame::new);
    }
}
