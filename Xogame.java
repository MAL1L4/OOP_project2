import javax.swing.*;
import java.awt.*;

class xo {
    idenfier ident;
    size size;
}

enum idenfier {
    X, O;
}

enum size {
    small, middle, large;
}

class Board {
    xo[][] board; // ประกาศ array 2 มิติ
    int countX;
    int countO;
    int countEmpty;

    // Constructor ของ Board เพื่อกำหนดขนาด 3x3
    public Board() {
        this.board = new xo[3][3]; // จองขนาดบอร์ดเป็น 3x3
        this.countEmpty = 9;       // เริ่มต้นมีช่องว่าง 9 ช่อง
        this.countX = 0;
        this.countO = 0;
    }

    // ---------- ส่วนที่เพิ่มเพื่อให้ GUI ตรวจผลเกมได้ ----------
    private idenfier at(int r, int c) {
        return board[r][c] == null ? null : board[r][c].ident;
    }

    private boolean line(idenfier a, idenfier b, idenfier c) {
        return a != null && a == b && b == c;
    }

    /** คืนผู้ชนะ (X หรือ O) ถ้ายังไม่มีคืน null */
    public idenfier winner() {
        for (int i = 0; i < 3; i++) {
            if (line(at(i, 0), at(i, 1), at(i, 2))) return at(i, 0); // แนวนอน
            if (line(at(0, i), at(1, i), at(2, i))) return at(0, i); // แนวตั้ง
        }
        if (line(at(0, 0), at(1, 1), at(2, 2))) return at(0, 0);     // แนวทแยง
        if (line(at(0, 2), at(1, 1), at(2, 0))) return at(1, 1);
        return null;
    }

    public boolean isFull() {
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                if (board[r][c] == null) return false;
        return true;
    }
}

// UI แบบ Console (ของเดิม)
class UI {
    public void displayBoard(Board board) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board.board[i][j] == null) {
                    System.out.print("[ ]");
                } else {
                    System.out.print("[" + board.board[i][j].ident + "]");
                }
            }
            System.out.println();
        }
    }
}

class player {
    int[] count = {2, 2, 2};
    idenfier ident;
    Board board;   // เก็บ reference ไว้

    public player(idenfier ident, Board board) {
        this.ident = ident;
        this.board = board;
    }

    public boolean placeNew(size s, int row, int col) {
        if (count[s.ordinal()] == 0) return false;       // หมากขนาดนี้หมดแล้ว
        if (board.board[row][col] != null) return false; // (เวอร์ชันนี้ยังไม่รองรับการซ้อน)

        xo piece = new xo();
        piece.ident = this.ident;
        piece.size = s;

        board.board[row][col] = piece;
        count[s.ordinal()]--;
        return true;
    }
}

// UI แบบกราฟิก (Swing)
class GameGUI {
    private Board board;
    private player playerX;
    private player playerO;
    private player current;
    private size selectedSize;
    private boolean gameOver;

    private final JFrame frame = new JFrame("XO Game");
    private final JButton[][] cells = new JButton[3][3];
    private final JToggleButton[] sizeButtons = new JToggleButton[3];
    private final JLabel status = new JLabel("", SwingConstants.CENTER);

    public GameGUI() {
        buildUI();
        newGame();
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void buildUI() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));

        // ด้านบน: สถานะ
        status.setFont(new Font("SansSerif", Font.BOLD, 18));
        status.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        frame.add(status, BorderLayout.NORTH);

        // กลาง: กระดาน 3x3
        JPanel grid = new JPanel(new GridLayout(3, 3, 4, 4));
        grid.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                JButton b = new JButton("");
                b.setPreferredSize(new Dimension(100, 100));
                b.setFocusPainted(false);
                final int row = r, col = c;
                b.addActionListener(e -> onCellClicked(row, col));
                cells[r][c] = b;
                grid.add(b);
            }
        }
        frame.add(grid, BorderLayout.CENTER);

        // ล่าง: เลือกขนาดหมาก + ปุ่มเริ่มใหม่
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        ButtonGroup group = new ButtonGroup();
        size[] all = size.values();
        for (int i = 0; i < all.length; i++) {
            final size s = all[i];
            JToggleButton tb = new JToggleButton(s.toString());
            tb.addActionListener(e -> selectedSize = s);
            sizeButtons[i] = tb;
            group.add(tb);
            bottom.add(tb);
        }
        JButton reset = new JButton("New Game");
        reset.addActionListener(e -> newGame());
        bottom.add(reset);
        frame.add(bottom, BorderLayout.SOUTH);
    }

    private void newGame() {
        board = new Board();
        playerX = new player(idenfier.X, board);
        playerO = new player(idenfier.O, board);
        current = playerX;
        selectedSize = size.small;
        sizeButtons[0].setSelected(true);
        gameOver = false;
        refresh();
        status.setText("Turn: " + current.ident);
    }

    private void onCellClicked(int r, int c) {
        if (gameOver) return;

        if (!current.placeNew(selectedSize, r, c)) {
            status.setText("วางไม่ได้! (ช่องไม่ว่าง หรือหมากขนาดนี้หมดแล้ว)");
            return;
        }

        idenfier w = board.winner();
        if (w != null) {
            gameOver = true;
            refresh();
            status.setText("🎉 " + w + " ชนะ!");
            return;
        }
        if (board.isFull()) {
            gameOver = true;
            refresh();
            status.setText("เสมอ!");
            return;
        }

        current = (current == playerX) ? playerO : playerX;
        refresh();
        status.setText("Turn: " + current.ident);
    }

    /** วาดกระดานและปุ่มเลือกขนาดใหม่ตามสถานะปัจจุบัน */
    private void refresh() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                xo p = board.board[r][c];
                JButton b = cells[r][c];
                if (p == null) {
                    b.setText("");
                    b.setToolTipText(null);
                } else {
                    b.setText(p.ident.toString());
                    b.setForeground(p.ident == idenfier.X ? new Color(30, 90, 200) : new Color(200, 40, 40));
                    int fontSize;
                    switch (p.size) {
                        case small:  fontSize = 24; break;
                        case middle: fontSize = 44; break;
                        default:     fontSize = 68; break;
                    }
                    b.setFont(new Font("SansSerif", Font.BOLD, fontSize));
                    b.setToolTipText(p.ident + " (" + p.size + ")");
                }
            }
        }

        // อัปเดตปุ่มเลือกขนาด ให้แสดงจำนวนที่เหลือของผู้เล่นคนปัจจุบัน
        size[] all = size.values();
        for (int i = 0; i < all.length; i++) {
            int left = current.count[i];
            sizeButtons[i].setText(all[i] + " (" + left + ")");
            sizeButtons[i].setEnabled(left > 0 && !gameOver);
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
    }
}

// main class สำหรับรันเกม
class Xogame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameGUI::new);
    }
}