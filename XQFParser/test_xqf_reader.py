#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import sys
from cchess.read_xqf import read_from_xqf

def print_game_info(game):
    if not game:
        print("Error: Failed to read XQF file")
        return False

    print("\nGame Information:")
    print("-----------------")
    for key, value in game.info.items():
        print(f"{key}: {value}")

    if game.annote:
        print("\nGame Annotation:")
        print("-----------------")
        print(game.annote)

    print("\nInitial Board State:")
    print("-----------------")
    print(game.init_board)

    print("\nMoves:")
    print("-----------------")
    moves = game.dump_iccs_moves
    print(moves)
    # for move in moves:
    #     print(f"Move {move_count}: {move}")
    #     if move.annote:
    #         print(f"  Annotation: {move.annote}")
    #     # move = move.next_move if move.next_move else None
    #     move_count += 1

    return True

def main():
    if len(sys.argv) < 2:
        xqf_file = "/Users/zfdang/workspaces/ChineseChess/棋谱/xqf/eleeye/SAMPLE.XQF"
        print("use default innput: {xqf_file}")
    else:
        xqf_file = sys.argv[1]
    
    if not os.path.exists(xqf_file):
        print(f"Error: File not found: {xqf_file}")
        sys.exit(1)

    try:
        print(f"Reading XQF file: {xqf_file}")
        game = read_from_xqf(xqf_file, True)
        if not print_game_info(game):
            sys.exit(1)

    except Exception as e:
        print(f"Error reading XQF file: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()