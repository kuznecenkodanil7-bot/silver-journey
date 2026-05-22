package ru.morisnmoto.minesweeper;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public class MinesweeperScreen extends Screen {
    private static final int HEADER_TOP = 14;
    private static final int BUTTON_Y = 34;
    private static final int BUTTON_W = 84;
    private static final int BUTTON_H = 20;
    private static final int BOARD_TOP = 78;
    private static final int PADDING = 18;

    private Difficulty difficulty = Difficulty.EASY;
    private Cell[][] board;
    private boolean minesPlaced;
    private boolean gameOver;
    private boolean won;
    private int revealedCells;
    private int flags;

    public MinesweeperScreen() {
        super(Text.translatable("screen.minesweeper_client.title"));
        restart();
    }

    @Override
    protected void init() {
        int totalWidth = BUTTON_W * 4 + 8 * 3;
        int x = (this.width - totalWidth) / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("Лёгкий"), button -> setDifficulty(Difficulty.EASY))
                .dimensions(x, BUTTON_Y, BUTTON_W, BUTTON_H)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Средний"), button -> setDifficulty(Difficulty.MEDIUM))
                .dimensions(x + (BUTTON_W + 8), BUTTON_Y, BUTTON_W, BUTTON_H)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сложный"), button -> setDifficulty(Difficulty.HARD))
                .dimensions(x + (BUTTON_W + 8) * 2, BUTTON_Y, BUTTON_W, BUTTON_H)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Заново"), button -> restart())
                .dimensions(x + (BUTTON_W + 8) * 3, BUTTON_Y, BUTTON_W, BUTTON_H)
                .build());
    }

    private void setDifficulty(Difficulty difficulty) {
        if (this.difficulty != difficulty) {
            this.difficulty = difficulty;
        }
        restart();
    }

    private void restart() {
        this.board = new Cell[difficulty.rows][difficulty.cols];
        for (int row = 0; row < difficulty.rows; row++) {
            for (int col = 0; col < difficulty.cols; col++) {
                this.board[row][col] = new Cell();
            }
        }
        this.minesPlaced = false;
        this.gameOver = false;
        this.won = false;
        this.revealedCells = 0;
        this.flags = 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);

        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Сапёр"), this.width / 2, HEADER_TOP, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(statusText()), this.width / 2, 60, statusColor());

        renderBoard(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void renderBoard(DrawContext context, int mouseX, int mouseY) {
        int cell = cellSize();
        int boardX = boardX();
        int boardY = BOARD_TOP;
        int boardWidth = difficulty.cols * cell;
        int boardHeight = difficulty.rows * cell;

        context.fill(boardX - 4, boardY - 4, boardX + boardWidth + 4, boardY + boardHeight + 4, 0xAA111111);

        for (int row = 0; row < difficulty.rows; row++) {
            for (int col = 0; col < difficulty.cols; col++) {
                int x = boardX + col * cell;
                int y = boardY + row * cell;
                drawCell(context, board[row][col], x, y, cell, row, col);
            }
        }

        int hoverCol = (mouseX - boardX) / Math.max(cell, 1);
        int hoverRow = (mouseY - boardY) / Math.max(cell, 1);
        if (inside(hoverRow, hoverCol) && mouseX >= boardX && mouseY >= boardY) {
            int x = boardX + hoverCol * cell;
            int y = boardY + hoverRow * cell;
            context.fill(x, y, x + cell, y + 1, 0xFFFFFFFF);
            context.fill(x, y + cell - 1, x + cell, y + cell, 0xFFFFFFFF);
            context.fill(x, y, x + 1, y + cell, 0xFFFFFFFF);
            context.fill(x + cell - 1, y, x + cell, y + cell, 0xFFFFFFFF);
        }
    }

    private void drawCell(DrawContext context, Cell cellData, int x, int y, int size, int row, int col) {
        int gap = Math.max(1, size / 12);
        int innerX1 = x + gap;
        int innerY1 = y + gap;
        int innerX2 = x + size - gap;
        int innerY2 = y + size - gap;

        if (cellData.revealed) {
            context.fill(x, y, x + size, y + size, 0xFF3B3B3B);
            context.fill(innerX1, innerY1, innerX2, innerY2, 0xFF5A5A5A);

            if (cellData.mine) {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("✹"), x + size / 2, y + textY(size), 0xFFFF5555);
            } else if (cellData.adjacentMines > 0) {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(String.valueOf(cellData.adjacentMines)), x + size / 2, y + textY(size), numberColor(cellData.adjacentMines));
            }
        } else {
            context.fill(x, y, x + size, y + size, 0xFF202020);
            context.fill(innerX1, innerY1, innerX2, innerY2, 0xFF808080);
            context.fill(innerX1 + 1, innerY1 + 1, innerX2 - 1, innerY2 - 1, 0xFF5F5F5F);

            if (cellData.flagged) {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("⚑"), x + size / 2, y + textY(size), 0xFFFFFF55);
            } else if (gameOver && cellData.mine) {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("✹"), x + size / 2, y + textY(size), 0xFFFF5555);
            }
        }
    }

    private int textY(int cellSize) {
        return Math.max(0, (cellSize - 8) / 2);
    }

    private String statusText() {
        String level = switch (difficulty) {
            case EASY -> "Лёгкий 9×9 / 10 мин";
            case MEDIUM -> "Средний 16×16 / 40 мин";
            case HARD -> "Сложный 30×16 / 99 мин";
        };

        if (won) {
            return "Победа! " + level + " | Флаги: " + flags + "/" + difficulty.mines;
        }
        if (gameOver) {
            return "Бум! Нажми «Заново» | " + level;
        }
        return level + " | Флаги: " + flags + "/" + difficulty.mines + " | ЛКМ открыть, ПКМ флаг";
    }

    private int statusColor() {
        if (won) return 0xFF55FF55;
        if (gameOver) return 0xFFFF5555;
        return 0xFFE0E0E0;
    }

    private int numberColor(int n) {
        return switch (n) {
            case 1 -> 0xFF55AAFF;
            case 2 -> 0xFF55FF55;
            case 3 -> 0xFFFF5555;
            case 4 -> 0xFFAA55FF;
            case 5 -> 0xFFFFAA00;
            case 6 -> 0xFF55FFFF;
            case 7 -> 0xFFFFFFFF;
            default -> 0xFFBBBBBB;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cell = cellSize();
        int col = (int) ((mouseX - boardX()) / cell);
        int row = (int) ((mouseY - BOARD_TOP) / cell);

        if (inside(row, col) && mouseX >= boardX() && mouseY >= BOARD_TOP) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                reveal(row, col);
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                toggleFlag(row, col);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void reveal(int row, int col) {
        if (gameOver || won || !inside(row, col)) return;

        Cell cell = board[row][col];
        if (cell.flagged || cell.revealed) return;

        if (!minesPlaced) {
            placeMines(row, col);
        }

        if (cell.mine) {
            cell.revealed = true;
            gameOver = true;
            revealAllMines();
            return;
        }

        floodReveal(row, col);
        checkWin();
    }

    private void toggleFlag(int row, int col) {
        if (gameOver || won || !inside(row, col)) return;

        Cell cell = board[row][col];
        if (cell.revealed) return;

        cell.flagged = !cell.flagged;
        flags += cell.flagged ? 1 : -1;
    }

    private void placeMines(int safeRow, int safeCol) {
        List<int[]> positions = new ArrayList<>();
        for (int row = 0; row < difficulty.rows; row++) {
            for (int col = 0; col < difficulty.cols; col++) {
                if (Math.abs(row - safeRow) <= 1 && Math.abs(col - safeCol) <= 1) {
                    continue;
                }
                positions.add(new int[]{row, col});
            }
        }

        Collections.shuffle(positions);
        for (int i = 0; i < difficulty.mines && i < positions.size(); i++) {
            int[] pos = positions.get(i);
            board[pos[0]][pos[1]].mine = true;
        }

        for (int row = 0; row < difficulty.rows; row++) {
            for (int col = 0; col < difficulty.cols; col++) {
                board[row][col].adjacentMines = countAdjacentMines(row, col);
            }
        }

        minesPlaced = true;
    }

    private int countAdjacentMines(int row, int col) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = row + dr;
                int nc = col + dc;
                if (inside(nr, nc) && board[nr][nc].mine) {
                    count++;
                }
            }
        }
        return count;
    }

    private void floodReveal(int startRow, int startCol) {
        Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startRow, startCol});

        while (!queue.isEmpty()) {
            int[] current = queue.remove();
            int row = current[0];
            int col = current[1];
            if (!inside(row, col)) continue;

            Cell cell = board[row][col];
            if (cell.revealed || cell.flagged || cell.mine) continue;

            cell.revealed = true;
            revealedCells++;

            if (cell.adjacentMines == 0) {
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        queue.add(new int[]{row + dr, col + dc});
                    }
                }
            }
        }
    }

    private void revealAllMines() {
        for (int row = 0; row < difficulty.rows; row++) {
            for (int col = 0; col < difficulty.cols; col++) {
                if (board[row][col].mine) {
                    board[row][col].revealed = true;
                }
            }
        }
    }

    private void checkWin() {
        if (revealedCells == difficulty.rows * difficulty.cols - difficulty.mines) {
            won = true;
            for (int row = 0; row < difficulty.rows; row++) {
                for (int col = 0; col < difficulty.cols; col++) {
                    if (board[row][col].mine && !board[row][col].flagged) {
                        board[row][col].flagged = true;
                        flags++;
                    }
                }
            }
        }
    }

    private boolean inside(int row, int col) {
        return row >= 0 && row < difficulty.rows && col >= 0 && col < difficulty.cols;
    }

    private int cellSize() {
        int byWidth = (this.width - PADDING * 2) / difficulty.cols;
        int byHeight = (this.height - BOARD_TOP - PADDING) / difficulty.rows;
        return Math.max(10, Math.min(18, Math.min(byWidth, byHeight)));
    }

    private int boardX() {
        return (this.width - difficulty.cols * cellSize()) / 2;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private enum Difficulty {
        EASY(9, 9, 10),
        MEDIUM(16, 16, 40),
        HARD(30, 16, 99);

        final int cols;
        final int rows;
        final int mines;

        Difficulty(int cols, int rows, int mines) {
            this.cols = cols;
            this.rows = rows;
            this.mines = mines;
        }
    }

    private static class Cell {
        boolean mine;
        boolean revealed;
        boolean flagged;
        int adjacentMines;
    }
}
