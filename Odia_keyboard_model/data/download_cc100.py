import urllib.request
import os
import lzma

def main():
    url = "http://data.statmt.org/cc-100/or.txt.xz"
    output_xz = "data/raw/cc100_odia.txt.xz"
    output_txt = "data/raw/cc100_odia.txt"

    print("Downloading CC-100 Odia corpus (~500MB compressed)...")
    print("This may take 10-30 minutes depending on your internet speed.")
    
    urllib.request.urlretrieve(url, output_xz)
    print(f"Downloaded to {output_xz}")

    print("Extracting text (this will take a few minutes)...")
    with lzma.open(output_xz, 'rt', encoding='utf-8', errors='ignore') as f:
        lines = f.readlines()

    print(f"Saving to {output_txt}...")
    with open(output_txt, 'w', encoding='utf-8') as f:
        for line in lines:
            if len(line.strip()) > 10:  # Only keep actual sentences
                f.write(line)

    print(f"Extracted {len(lines)} lines to {output_txt}")

if __name__ == "__main__":
    main()