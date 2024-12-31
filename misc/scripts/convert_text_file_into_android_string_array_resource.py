#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse


def convert(file_path, string_array_name):
    try:
        # Read the content of the file
        with open(file_path, 'r', encoding='utf-8') as file:
            lines = file.readlines()

        # strip trailing newline character
        lines = [line.rstrip('\n') for line in lines]

        # enclose by xml tag
        lines = ["        <item>{}</item>".format(line) for line in lines if line]

        # Escape single quotes in each line
        lines = [line.replace("'", "\\'") for line in lines]

        # print the modified content
        print("    <string-array name=\"{}\">\n{}\n    </string-array>"
              .format(string_array_name, '\n'.join(lines)))

    except FileNotFoundError:
        print(f"Error: The file '{file_path}' was not found.")
    except IOError as e:
        print(f"Error: An I/O error occurred. {e}")


def main():
    parser = argparse.ArgumentParser(description="Convert to Android resources string array")
    parser.add_argument("-n", "--name", default="", help="string array name")
    parser.add_argument('file_path', type=str, help='The path to the file to be processed.')

    args = parser.parse_args()
    convert(args.file_path, args.name)


if __name__ == '__main__':
    main()
