/* Skeleton Copyright (C) 2015, 2020 Paul N. Hilfinger and the Regents of the
 * University of California.  All rights reserved. */
package loa;


import static loa.Piece.*;
import java.util.ArrayList;
import java.util.List;
/** An automated Player.
 *  @author Ryan Van de Water
 */
class MachinePlayer extends Player {

    /** A position-score magnitude indicating a win (for white if positive,
     *  black if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 20;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;




    /** A new MachinePlayer with no piece or controller (intended to produce
     *  a template). */
    MachinePlayer() {
        this(null, null);
    }

    /** A MachinePlayer that plays the SIDE pieces in GAME. */
    MachinePlayer(Piece side, Game game) {
        super(side, game);
    }

    @Override
    String getMove() {
        Move choice;
        assert side() == getGame().getBoard().turn();
        choice = searchForMove();
        getGame().reportMove(choice);
        return choice.toString();
    }

    @Override
    Player create(Piece piece, Game game) {
        return new MachinePlayer(piece, game);
    }

    @Override
    boolean isManual() {
        return false;
    }

    /** Getter method for the holder variable.
     * @return the value in the holder variable*/
    int holder() {
        return holder;
    }

    /** Return a move after searching the game tree to DEPTH>0 moves
     *  from the current position. Assumes the game is not over. */
    private Move searchForMove() {
        Board work = new Board(getBoard());
        int value;
        assert side() == work.turn();
        _foundMove = null;
        if (side() == WP) {
            value = findMove(work, chooseDepth(), true, 1, -INFTY, INFTY);
        } else {
            value = findMove(work, chooseDepth(), true, -1, -INFTY, INFTY);
        }
        return _foundMove;
    }

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _foundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > ALPHA if SENSE==1,
     *  and minimal value or value < BETA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove. */
    private int findMove(Board board, int depth, boolean saveMove,
                         int sense, int alpha, int beta) {
        holder = heuristicFunction(board, sense);
        if (sense == 1) {
            return Math.max(findMax(board, sense, 1, alpha, beta),
                    findMax(board, sense, depth, alpha, beta));
        } else {
            return Math.min(findMin(board, sense, 1, alpha, beta),
                    findMin(board, sense, depth, alpha, beta));
        }
    }

    /** This function is called when the maximizing player is
     *  searching for a move.
     * @param board - the current position.
     * @param sense - the player we are searching for.
     * @param depth - the level at which to search at
     * @param alpha - the current value for alpha
     * @param beta - the current value for beta
     * @return the value of the optimal move for this player. */
    private int findMax(Board board, int sense, int depth,
                        int alpha, int beta) {
        if (depth == 0 || board.gameOver()) {
            return oneDepthMax(board, sense, alpha, beta);
        } else {
            int optimal = -INFTY;
            for (Move m : board.legalMoves()) {
                board.makeMove(m);
                int response = findMin(board, sense, depth - 1, alpha, beta);
                board.retract();
                if (response >= optimal) {
                    optimal = response;
                    m.setValue(optimal);
                    alpha = Math.max(alpha, response);
                    if (sense == 1 && _foundMove == null) {
                        _foundMove = m;
                    } else if (sense == 1 && m.value() > _foundMove.value()) {
                        _foundMove = m;
                    }
                } else {
                    alpha = Math.max(alpha, optimal);
                }
                if (beta <= alpha) {
                    break;
                }
            }
            return optimal;
        }
    }

    /** This function is called when the minimizing player is
     * searching for a move.
     * @param board - the current position.
     * @param sense - the player we are searching for.
     * @param depth - the level at which to search at
     * @param alpha - the current value for alpha
     * @param beta - the current value for beta
     * @return the value of the optimal move for this player. */
    private int findMin(Board board, int sense, int depth,
                        int alpha, int beta) {
        if (depth == 0 || board.gameOver()) {
            return oneDepthMin(board, sense, alpha, beta);
        } else {
            int optimal = INFTY;
            for (Move m : board.legalMoves()) {
                board.makeMove(m);
                int response = findMax(board, sense, depth - 1, alpha, beta);
                board.retract();
                if (response <= optimal) {
                    optimal = response;
                    m.setValue(optimal);
                    beta = Math.min(beta, response);
                    if (sense == -1 && _foundMove == null) {
                        _foundMove = m;
                    } else if (sense == -1 && m.value() < _foundMove.value()) {
                        _foundMove = m;
                    }
                } else {
                    beta = Math.min(beta, optimal);
                }
                if (beta <= alpha) {
                    break;
                }
            }
            return optimal;
        }
    }

    /** This function looks at all the possible moves and
     * ranks them according to the heuristic function
     * at the bottom of our tree.
     * @param board - the current position
     * @param sense - who is playing
     * @param alpha - the value of alpha at this position
     * @param beta - the value of beta at this position
     * @return the value of the optimal move at the bottom
     * of the tree
     */
    private int oneDepthMax(Board board, int sense, int alpha, int beta) {
        if (board.gameOver()) {
            return heuristicFunction(board, sense);
        } else {
            int bestSoFar = -INFTY;
            int origDist = averageDist(board, WP);
            for (Move m : board.legalMoves()) {
                board.makeMove(m);
                int score;
                int nowDist = averageDist(board, WP);
                if (nowDist < origDist) {
                    score = heuristicFunction(board, sense)
                            + zone2Rand[getGame().randInt(s)];
                } else {
                    score = heuristicFunction(board, sense);
                }
                if (score >= bestSoFar) {
                    bestSoFar = score;
                    alpha = Math.max(alpha, score);
                } else {
                    alpha = Math.max(alpha, bestSoFar);
                }
                board.retract();
                if (beta <= alpha) {
                    break;
                }
            }
            return bestSoFar;
        }
    }

    /** Returns the value of the moves at the bottom of the tree for
     * the minimizing player.
     * @param board - current position
     * @param sense - indicates who's playing
     * @param alpha - score for the board, seeking to maximize
     * @param beta - score for the board, seeking to minimize
     * @return the best value after making the legal moves at
     * depth 0.
     */
    private int oneDepthMin(Board board, int sense, int alpha, int beta) {
        if (board.gameOver()) {
            return heuristicFunction(board, sense);
        } else {
            int bestSoFar = INFTY;
            int origDist = averageDist(board, BP);
            for (Move m : board.legalMoves()) {
                board.makeMove(m);
                int score;
                int nowDist = averageDist(board, BP);
                if (nowDist < origDist) {
                    score = heuristicFunction(board, sense)
                            - zone2Rand[getGame().randInt(s)];
                } else {
                    score = heuristicFunction(board, sense);
                }
                if (score <= bestSoFar) {
                    bestSoFar = score;
                    beta = Math.min(beta, score);
                } else {
                    beta = Math.min(beta, bestSoFar);
                }
                board.retract();
                if (beta <= alpha) {
                    break;
                }
            }
            return bestSoFar;
        }
    }

    /** Sets the search depth.
     * @return the max search depth we want to explore */
    private int chooseDepth() {
        return 2;
    }

    /** A function that uses a variety of metrics to place a
     * numeric score to the position of the board.
     * @param board - the position of the board.
     * @param sense - who's playing at the moment.
     * @return average distances between player's pieces
     */
    private int heuristicFunction(Board board, int sense) {
        if (sense == 1) {
            return whitePoints(board);
        } else {
            return blackPoints(board);
        }
    }

    /** Get the white point total.
     * @param board - the current position
     * @return the white point total.
     */
    int whitePoints(Board board) {
        ArrayList<Square> wTeam = board.getWhiteSquares();
        List<Integer> wReg = board.getRegionSizes(WP);
        int sumWReg = 0;
        int pointTotal = 0;
        int i = 0;
        while (i < wReg.size()) {
            sumWReg += wReg.get(i);
            i++;
        }
        if (board.gameOver()) {
            if (board.winner().equals(WP)) {
                return INFTY;
            } else {
                return -INFTY;
            }
        }
        for (Square sq : wTeam) {
            if (sq.col() >= 1 && sq.col() <= 6) {
                pointTotal += zone1Rand[getGame().randInt(s)];
                if (sq.col() >= 2 && sq.col() <= 5) {
                    pointTotal += zone2Rand[getGame().randInt(s)];
                    if (sq.row() >= 2 && sq.row() <= 4) {
                        pointTotal += zone3Rand[getGame().randInt(s)];
                    }
                }
            } else {
                pointTotal -= 100;
            }
        }
        if (wReg.get(0) < lower * sumWReg) {
            pointTotal -= 100;
        }
        if (wReg.size() > 3) {
            pointTotal -= 100;
        }
        if (wReg.get(0) > upper * sumWReg) {
            if (wReg.size() > 3) {
                pointTotal -= medium;
            } else {
                pointTotal += zone3Rand[getGame().randInt(s)];
            }
        }
        if (wReg.size() == 2) {
            pointTotal += zone2Rand[getGame().randInt(s)];
        }
        if (pointTotal > holder) {
            if (getGame().randInt(11) >= 5) {
                pointTotal += medium;
            } else {
                pointTotal += (medium / 2);
            }
        }
        return pointTotal;
    }

    /** Return the points for the black team.
     * @param board - the current position
     * @return the black team point total
     */
    int blackPoints(Board board) {
        ArrayList<Square> bTeam = board.getBlackSquares();
        List<Integer> bReg = board.getRegionSizes(BP);
        int sumBReg = 0;
        int pointTotal = 0;
        int i = 0;
        while (i < bReg.size()) {
            sumBReg += bReg.get(i);
            i++;
        }
        if (board.gameOver()) {
            if (board.winner().equals(BP)) {
                return -INFTY;
            } else {
                return INFTY;
            }
        }
        for (Square sq : bTeam) {
            if (sq.row() >= 1 && sq.row() <= 6) {
                pointTotal -= zone1Rand[getGame().randInt(s)];
                if (sq.row() >= 2 && sq.row() <= 5) {
                    pointTotal -= zone2Rand[getGame().randInt(s)];
                    if (sq.col() >= 2 && sq.col() <= 4) {
                        pointTotal -= zone3Rand[getGame().randInt(s)];
                    }
                }
            } else {
                pointTotal += 100;
            }
        }
        if (bReg.get(0) < lower * sumBReg) {
            pointTotal += 100;
        }
        if (bReg.size() > 3) {
            pointTotal += 100;
        }
        if (bReg.get(0) > upper * sumBReg) {
            if (bReg.size() > 3) {
                pointTotal += medium;
            } else {
                pointTotal -= zone3Rand[getGame().randInt(s)];
            }
        }
        if (bReg.size() == 2) {
            pointTotal -= zone2Rand[getGame().randInt(s)];
        }
        if (pointTotal < holder) {
            if (getGame().randInt(11) >= 5) {
                pointTotal -= medium;
            } else {
                pointTotal -= (medium / 2);
            }
        }
        return pointTotal;
    }

    /** Returns the average distance between pieces on the
     * board.
     * @param board - position
     * @param p - indicates the player playing
     * @return average distance between the pieces
     */
    int averageDist(Board board, Piece p) {
        ArrayList<Square> wTeam = board.getWhiteSq();
        ArrayList<Square> bTeam = board.getBlackSq();
        int sumDists = 0;
        int numDists = 0;
        if (p.equals(WP)) {
            for (Square w : wTeam) {
                for (Square other : wTeam) {
                    if (w.equals(other)) {
                        continue;
                    }
                    sumDists += w.distance(other);
                    numDists += 1;
                }
            }
        } else {
            for (Square b : bTeam) {
                for (Square other : bTeam) {
                    if (b.equals(other)) {
                        continue;
                    }
                    sumDists += b.distance(other);
                    numDists += 1;
                }
            }
        }
        return sumDists / numDists;
    }

    /** Used to convey moves discovered by findMove. */
    private Move _foundMove;
    /** Used to access the last score of the board. */
    private int holder;
    /** Integer passed into randInt. */
    private final int s = 5;
    /** Integer list that gives a low score range. */
    private final int[] zone1Rand = {1, 2, 3, 4, 5};
    /** Integer list that gives a medium score range. */
    private final int[] zone2Rand = {20, 25, 30, 35, 40};
    /** Integer list that gives a high score range. */
    private final int[] zone3Rand = {90, 95, 100, 105, 110};
    /** Indicates the depth level at which to search.*/
    private int d;
    /** The percentage of the number of pieces that the first
     * region size must be in order to not incur a loss of points.
     */
    private final double lower = .4;
    /** The percentage of the number of pieces that the first
     * region size must be in order to gain points.
     */
    private final double upper = .8;
    /** A medium amount of points to incur as a loss/gain. */
    private final int medium = 50;
}
