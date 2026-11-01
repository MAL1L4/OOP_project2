import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;

class xo {
    idenfier ident;
    size size;
}

enum idenfier {
    X, O;

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

// UI แบบกราฟิก (Swing)
class GameGUI {
    private Board board;
    private player playerX;
    private player playerO;
    private player current;
    private size selectedSize;
    private boolean placeMode;   // true = วางหมากใหม่, false = ย้ายหมาก
    private int fromR, fromC;    // ช่องที่เลือกไว้ตอนย้าย (-1 = ยังไม่เลือก)
    private boolean gameOver;

    private final JFrame frame = new JFrame("GobblePets");
    private final JButton[][] cells = new JButton[3][3];
    private final JToggleButton[] sizeButtons = new JToggleButton[3];
    private final JToggleButton placeBtn = new JToggleButton("วางหมากใหม่");
    private final JToggleButton moveBtn = new JToggleButton("ย้ายหมาก");
    private final JLabel status = new JLabel("", SwingConstants.CENTER);
    private Border defaultBorder;

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
        status.setFont(new Font("SansSerif", Font.BOLD, 16));
        status.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        frame.add(status, BorderLayout.NORTH);

        // กลาง: กระดาน 3x3
        JPanel grid = new JPanel(new GridLayout(3, 3, 4, 4));
        grid.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                JButton b = new JButton("");
                b.setPreferredSize(new Dimension(110, 110));
                b.setFocusPainted(false);
                final int row = r, col = c;
                b.addActionListener(e -> onCellClicked(row, col));
                cells[r][c] = b;
                grid.add(b);
            }
        }
        defaultBorder = cells[0][0].getBorder();
        frame.add(grid, BorderLayout.CENTER);

        // ล่าง: โหมด + เลือกขนาด + เริ่มใหม่
        JPanel bottom = new JPanel(new GridLayout(2, 1, 4, 4));

        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(placeBtn);
        modeGroup.add(moveBtn);
        placeBtn.addActionListener(e -> setMode(true));
        moveBtn.addActionListener(e -> setMode(false));
        modeRow.add(placeBtn);
        modeRow.add(moveBtn);
        frame.add(bottom, BorderLayout.SOUTH);

        JPanel sizeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        ButtonGroup sizeGroup = new ButtonGroup();
        size[] all = size.values();
        for (int i = 0; i < all.length; i++) {
            final size s = all[i];
            JToggleButton tb = new JToggleButton(s.toString());
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
        status.setText("Turn: " + current.ident + " — เลือกขนาดแล้วคลิกช่องเพื่อวางหมาก");
    }

    private void setMode(boolean place) {
        placeMode = place;
        fromR = -1;
        fromC = -1;
        refresh();
        status.setText("Turn: " + current.ident + (place
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
                finish(current.ident + " เรียงครบ 3 ตัว ชนะ!");
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
            status.setText("Turn: " + current.ident + " — คลิกหมากของคุณที่อยู่บนสุดเพื่อย้าย");
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
            finish(current.ident + " ยกหมากออก ทำให้ " + opp + " เรียงครบ 3 ตัว " + opp + " ชนะ!");
            return;
        }

        // 2) วางที่ช่องใหม่ แล้วเช็กว่าเราชนะหรือไม่
        board.place(piece, tr, tc);
        if (board.hasWon(current.ident)) {
            fromR = -1;
            fromC = -1;
            finish(current.ident + " เรียงครบ 3 ตัว ชนะ!");
            return;
        }
        nextTurn();
    }

    private void finish(String message) {
        gameOver = true;
        refresh();
        status.setText("🎉 " + message);
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
            status.setText(skipped + " ไม่มีตาเดินที่ทำได้ ข้ามตา — Turn: " + current.ident);
            return;
        }

        refresh();
        status.setText("Turn: " + current.ident);
    }

    /** วาดกระดานและปุ่มต่าง ๆ ตามสถานะปัจจุบัน */
    private void refresh() {
        // ถ้าไม่เหลือหมากนอกกระดานแล้ว ต้องใช้โหมดย้ายเท่านั้น
        if (!current.hasSupply() && placeMode) {
            placeMode = false;
            moveBtn.setSelected(true);
        }

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                xo p = board.top(r, c);
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

                    // tooltip แสดงหมากที่ซ้อนกันอยู่ (บนสุด -> ล่างสุด)
                    StringBuilder sb = new StringBuilder("บนสุด→ล่างสุด: ");
                    for (xo q : board.board[r][c]) {
                        sb.append(q.ident).append('(').append(q.size).append(") ");
                    }
                    b.setToolTipText(sb.toString());
                }
                // ไฮไลต์ช่องที่เลือกไว้เพื่อย้าย
                b.setBorder((r == fromR && c == fromC)
                        ? BorderFactory.createLineBorder(new Color(255, 140, 0), 4)
                        : defaultBorder);
            }
        }

        placeBtn.setEnabled(current.hasSupply() && !gameOver);
        moveBtn.setEnabled(!gameOver);

        // ปุ่มเลือกขนาด: แสดงจำนวนที่เหลือ และใช้ได้เฉพาะโหมดวางหมากใหม่
        size[] all = size.values();
        for (int i = 0; i < all.length; i++) {
            int left = current.count[i];
            sizeButtons[i].setText(all[i] + " (" + left + ")");
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
    }
}

// main class สำหรับรันเกม
class Xogame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameGUI::new);
    }
}