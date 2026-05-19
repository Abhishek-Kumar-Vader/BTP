import os

wiki_path = "data/raw/odia_corpus.txt"
cc100_path = "data/raw/cc100_odia.txt"
combined_path = "data/raw/combined_corpus.txt"

print("Combining datasets...")
with open(combined_path, 'w', encoding='utf-8') as outfile:
    # 1. Write Wikipedia data
    print(f"Adding {wiki_path}...")
    with open(wiki_path, 'r', encoding='utf-8') as infile:
        outfile.write(infile.read())
        outfile.write("\n")
        
    # 2. Write CC-100 data
    print(f"Adding {cc100_path}...")
    with open(cc100_path, 'r', encoding='utf-8') as infile:
        outfile.write(infile.read())

print(f" Data combined successfully into {combined_path}!")