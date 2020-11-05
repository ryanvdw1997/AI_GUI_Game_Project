/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

import java.util.regex.Pattern;

import static loa.Piece.*;
import static loa.Square.*;

/** Represents the state of a game of Lines of Action.
 *  @author Ryan Van de Water
 */
class Board {

    /** Default number of moves for each side that results in a draw. */
    static final int DEFAULT_MOVE_LIMIT = 60;

    /** Pattern describing a valid square designator (cr). */
    static final Pattern ROW_COL = Pattern.compile("^[a-h][1-8]$");

    /** A Board whose initial contents are taken from INITIALCONTENTS
     *  and in which the player playing TURN is to move. The resulting
     *  Board has
     *        get(col, row) == INITIALCONTENTS[row][col]
     *  Assumes that PLAYER is not null and INITIALCONTENTS is 8x8.
     *
     *  CAUTION: The natural written notation for arrays initializers puts
     *  the BOTTOM row of INITIALCONTENTS at the top.
     */
    Board(Piece[][] initialContents, Piece turn) {
        initialize(initialContents, turn);
    }

    /** A new board in the standard initial position. */
    Board() {
        this(INITIAL_PIECES, BP);
    }

    /** A Board whose initial contents and state are copied from
     *  BOARD. */
    Board(Board board) {
        this();
        copyFrom(board);
    }

    /** Set my state to CONTENTS with SIDE to move. */
    void initialize(Piece[][] contents, Piece side) {
        if (contents.equals(INITIAL_PIECES)) {
            _winner = null;
            _moveLimit = DEFAULT_MOVE_LIMIT;
        }
        for (int i = 0; i < contents.length; i++) {
            for (int j = 0; j < contents[i].length; j++) {
                Piece m = contents[i][j];
                state[i][j] = m;
            }
        }
        for (int i = 0; i < contents.length; i++) {
            for (int j = 0; j < contents[i].length; j++) {
                int index = (i << 3) + j;
                Piece m = contents[i][j];
                _board[index] = m;
            }
        }
        _turn = side;
        if (_moveLimit == 0) {
            _moveLimit = DEFAULT_MOVE_LIMIT;
        }
        whiteSquares = getWhiteSq();
        blackSquares = getBlackSq();
        computeRegions();
    }

    /** Set me to the initial configuration. */
    void clear() {
        initialize(INITIAL_PIECES, BP);
    }

    /** Set my state to a copy of BOARD. */
    void copyFrom(Board board) {
        if (board == this) {
            return;
        }
        initialize(board.state, board.turn());
        this._moveLimit = board._moveLimit;
        this._winnerKnown = board._winnerKnown;
        this._winner = board._winner;
        this._subsetsInitialized = board._subsetsInitialized;

    }

    /** Return the contents of the square at SQ. */
    Piece get(Square sq) {
        return _board[sq.index()];
    }

    /** Set the square at SQ to V and set the side that is to move next
     *  to NEXT, if NEXT is not null. */
    void set(Square sq, Piece v, Piece next) {
        state[sq.row()][sq.col()] = v;
        if (next != null) {
            initialize(state, next);
        } else {
            initialize(state, _turn);
        }
    }

    /** Set the square at SQ to V, without modifying the side that
     *  moves next. */
    void set(Square sq, Piece v) {
        set(sq, v, null);
    }

    /** Set limit on number of moves (before tie results) to LIMIT. */
    void setMoveLimit(int limit) {
        _moveLimit = limit * 2;
        _winnerKnown = false;
    }

    /** Assuming isLegal(MOVE), make MOVE. Assumes MOVE.isCapture()
     *  is false. */
    void makeMove(Move move) {
        assert isLegal(move);
        Square fromSq = move.getFrom();
        Square toSq = move.getTo();
        Piece from = get(fromSq);
        Piece to = get(toSq);
        if (to.equals(EMP)) {
            move = Move.mv(fromSq, toSq, false);
        } else {
            move = Move.mv(fromSq, toSq, true);
        }
        state[fromSq.row()][fromSq.col()] = EMP;
        state[toSq.row()][toSq.col()] = from;
        _subsetsInitialized = false;
        initialize(state, _turn.opposite());
        _moves.add(move);
    }

    /** Retract (unmake) one move, returning to the state immediately before
     *  that move.  Requires that movesMade () > 0. */
    void retract() {
        assert movesMade() > 0;
        Move last = _moves.get(movesMade() - 1);
        Piece to = get(last.getTo());
        Square toSq = last.getTo();
        Square fromSq = last.getFrom();
        if (!last.isCapture()) {
            state[toSq.row()][toSq.col()] = EMP;
        } else {
            if (to.equals(BP)) {
                state[toSq.row()][toSq.col()] = WP;
            } else {
                state[toSq.row()][toSq.col()] = BP;
            }
        }
        state[fromSq.row()][fromSq.col()] = to;
        initialize(state, _turn.opposite());
        _moves.remove(last);
    }

    /** Return the Piece representing who is next to move. */
    Piece turn() {
        return _turn;
    }

    /** Return true iff FROM - TO is a legal move for the player currently on
     *  move. */
    boolean isLegal(Square from, Square to) {
        Piece enemy = get(from).opposite();
        Piece end = get(to);
        Piece fromP = get(from);
        if (!fromP.equals(_turn)) {
            return false;
        }
        if (!from.isValidMove(to)) {
            return false;
        }
        int dir = from.direction(to);
        int pieceCount = numPieces(from, to, dir);
        if (fromP.equals(end)) {
            return false;
        }  else if (dir == -1) {
            return false;
        } else if (from.distance(to) != pieceCount) {
            return false;
        } else {
            Square next = from.moveDest(dir, 1);
            while (next != null) {
                if (next.equals(to)) {
                    return true;
                } else if (get(next).equals(enemy)) {
                    return false;
                }
                next = next.moveDest(dir, 1);
            }
            return false;
        }
    }

    /** Return true iff MOVE is legal for the player currently on move.
     *  The isCapture() property is ignored. */
    boolean isLegal(Move move) {
        return isLegal(move.getFrom(), move.getTo());
    }

    /** Return a sequence of all legal moves from this position. */
    List<Move> legalMoves() {
        List<Move> legals = new ArrayList<>();
        for (Square from : ALL_SQUARES) {
            for (Square other : ALL_SQUARES) {
                if (get(from).equals(_turn)) {
                    if (isLegal(from, other)) {
                        if (get(other).equals(_turn.opposite())) {
                            Move m = Move.mv(from, other, true);
                            legals.add(Move.mv(from, other, true));
                        } else {
                            legals.add(Move.mv(from, other, false));
                        }
                    }
                }
            }
        }
        return legals;
    }

    /** Return true iff the game is over (either player has all his
     *  pieces continguous or there is a tie). */
    boolean gameOver() {
        return winner() != null;
    }

    /** Return true iff SIDE's pieces are continguous. */
    boolean piecesContiguous(Piece side) {
        return getRegionSizes(side).size() == 1;
    }

    /** Return the winning side, if any.  If the game is not over, result is
     *  null.  If the game has ended in a tie, returns EMP. */
    Piece winner() {
        if (!_winnerKnown) {
            if (piecesContiguous(_turn.opposite())) {
                _winner = _turn.opposite();
                _winnerKnown = true;
            } else if (piecesContiguous(_turn)) {
                _winner = _turn;
                _winnerKnown = true;
            } else if (movesMade() >= _moveLimit) {
                _winner = EMP;
                _winnerKnown = true;
            } else {
                _winnerKnown = false;
                _winner = null;
            }
        }
        return _winner;
    }

    /** Return the total number of moves that have been made (and not
     *  retracted).  Each valid call to makeMove with a normal move increases
     *  this number by 1. */
    int movesMade() {
        return _moves.size();
    }

    @Override
    public boolean equals(Object obj) {
        Board b = (Board) obj;
        return Arrays.deepEquals(_board, b._board) && _turn == b._turn;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(_board) * 2 + _turn.hashCode();
    }

    @Override
    public String toString() {
        Formatter out = new Formatter();
        out.format("===%n");
        for (int r = BOARD_SIZE - 1; r >= 0; r -= 1) {
            out.format("    ");
            for (int c = 0; c < BOARD_SIZE; c += 1) {
                out.format("%s ", get(sq(c, r)).abbrev());
            }
            out.format("%n");
        }
        out.format("Next move: %s%n===", turn().fullName());
        return out.toString();
    }

    /** Return true if a move from FROM to TO is blocked by an opposing
     *  piece or by a friendly piece on the target square. */
    private boolean blocked(Square from, Square to) {
        return false;
    }

    /** Return the size of the as-yet unvisited cluster of squares
     *  containing P at and adjacent to SQ.  VISITED indicates squares that
     *  have already been processed or are in different clusters.  Update
     *  VISITED to reflect squares counted. */
    private int numContig(Square sq, boolean[][] visited, Piece p) {
        int total = 0;
        if (exists(sq.row(), sq.col()) && !visited[sq.row()][sq.col()]) {
            if (!get(sq).equals(EMP)) {
                if (get(sq).equals(p)) {
                    total += 1;
                    visited[sq.row()][sq.col()] = true;
                    for (Square adj : sq.adjacent()) {
                        total += numContig(adj, visited, p);
                    }
                }
            }
        }
        return total;
    }

    /** Set the values of _whiteRegionSizes and _blackRegionSizes. */
    private void computeRegions() {
        if (_subsetsInitialized) {
            return;
        }
        _whiteRegionSizes.clear();
        _blackRegionSizes.clear();
        boolean[][] wTrack = new boolean[BOARD_SIZE][BOARD_SIZE];
        for (Square sq : whiteSquares) {
            Piece sqP = get(sq);
            int regionSize = numContig(sq, wTrack, sqP);
            if (regionSize != 0) {
                _whiteRegionSizes.add(regionSize);
            }
        }
        boolean[][] bTrack = new boolean[BOARD_SIZE][BOARD_SIZE];
        for (Square sq : blackSquares) {
            Piece sqP = get(sq);
            int regionSize = numContig(sq, bTrack, sqP);
            if (regionSize != 0) {
                _blackRegionSizes.add(regionSize);
            }
        }
        Collections.sort(_whiteRegionSizes, Collections.reverseOrder());
        Collections.sort(_blackRegionSizes, Collections.reverseOrder());
        _subsetsInitialized = true;
    }

    /** Return the sizes of all the regions in the current union-find
     *  structure for side S.
     */
    List<Integer> getRegionSizes(Piece s) {
        computeRegions();
        if (s == WP) {
            return _whiteRegionSizes;
        } else {
            return _blackRegionSizes;
        }
    }

    /** Returns the number of pieces along the line of action.
     * @param from - the Square we are moving from
     * @param to - the Square we are moving to
     * @param dir - the direction in which we are moving
     * return the number of pieces along this line of action*/
    int numPieces(Square from, Square to, int dir) {
        int counter = 1;
        int opposite = dir - 4;
        if (dir - 4 < 0) {
            opposite = dir + 4;
        }
        Square next = from.moveDest(dir, 1);
        while (next != null) {
            if (!get(next).equals(EMP)) {
                counter += 1;
            }
            next = next.moveDest(dir, 1);
        }
        Square back = from.moveDest(opposite, 1);
        while (back != null) {
            if (!get(back).equals(EMP)) {
                counter += 1;
            }
            back = back.moveDest(opposite, 1);
        }
        return counter;
    }

    /** Returns the number of WP's on the board.
     * @return the list of white squares on the board*/
    public ArrayList<Square> getWhiteSq() {
        ArrayList<Square> holder = new ArrayList<>();
        for (Square sq : ALL_SQUARES) {
            if (get(sq).equals(WP)) {
                holder.add(sq);
            }
        }
        return holder;
    }

    /** Returns BP's on the board.
     * @return the list of black squares on the board*/
    public ArrayList<Square> getBlackSq() {
        ArrayList<Square> holder = new ArrayList<>();
        for (Square sq : ALL_SQUARES) {
            if (get(sq).equals(BP)) {
                holder.add(sq);
            }
        }
        return holder;
    }

    /** Getter method for WHITESQUARES.
     * @return the list of white squares on the board */
    ArrayList<Square> getWhiteSquares() {
        return whiteSquares;
    }

    /** Getter method for BLACKSQUARES.
     * @return the number of black squares on the board */
    ArrayList<Square> getBlackSquares() {
        return blackSquares;
    }

    /** Getter method for state variable.
     * @return the state of the board */
    Piece[][] state() {
        return state;
    }

    /** The standard initial configuration for Lines of Action (bottom row
     *  first). */
    static final Piece[][] INITIAL_PIECES = {
            { EMP, BP,  BP,  BP,  BP,  BP,  BP,  EMP },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { EMP, BP,  BP,  BP,  BP,  BP,  BP,  EMP }
    };

    /** Current contents of the board.  Square S is at _board[S.index()]. */
    private final Piece[] _board = new Piece[BOARD_SIZE  * BOARD_SIZE];

    /** List of all unretracted moves on this board, in order. */
    private final ArrayList<Move> _moves = new ArrayList<>();
    /** Current side on move. */
    private Piece _turn;
    /** Limit on number of moves before tie is declared.  */
    private int _moveLimit;
    /** True iff the value of _winner is known to be valid. */
    private boolean _winnerKnown;
    /** Cached value of the winner (BP, WP, EMP (for tie), or null (game still
     *  in progress).  Use only if _winnerKnown. */
    private Piece _winner;

    /** True iff subsets computation is up-to-date. */
    private boolean _subsetsInitialized;

    /** List of the sizes of continguous clusters of pieces, by color. */
    private final ArrayList<Integer>
            _whiteRegionSizes = new ArrayList<>(),
            _blackRegionSizes = new ArrayList<>();

    /** Tells us the current state of the board, initially
     * is equal to INITIAL_PIECES.
     */
    private Piece[][] state = {
            { EMP, BP,  BP,  BP,  BP,  BP,  BP,  EMP },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { WP,  EMP, EMP, EMP, EMP, EMP, EMP, WP  },
            { EMP, BP,  BP,  BP,  BP,  BP,  BP,  EMP }
    };

    /** List of Squares corresponding to the
     * white pieces on the current state of
     * the board.
     */
    private ArrayList<Square> whiteSquares;

    /** List of Squares corresponding to the
     * black pieces on the current state of the
     * board.
     */
    private ArrayList<Square> blackSquares;
}
