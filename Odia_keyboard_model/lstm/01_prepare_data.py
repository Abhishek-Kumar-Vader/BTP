import os
import json
import re
import numpy as np
from collections import Counter
from tqdm import tqdm

# ─── Configuration ─────────────────────────────────────────────────────────────
CORPUS_PATH = "data/raw/combined_corpus.txt"
PROCESSED_DIR = "data/processed"
SEQUENCE_LEN  = 5
VOCAB_SIZE    = 15000
MIN_FREQ      = 2
os.makedirs(PROCESSED_DIR, exist_ok=True)

PAD_TOKEN = "<PAD>"
UNK_TOKEN = "<UNK>"
BOS_TOKEN = "<BOS>"

def tokenize_odia(text: str) -> list:
    text = re.sub(r'[^\u0B00-\u0B7F\s]', ' ', text)
    tokens = text.split()
    return [t.strip() for t in tokens if t.strip()]

def build_vocabulary(corpus_path: str):
    print("Building vocabulary...")
    word_counts = Counter()
    with open(corpus_path, 'r', encoding='utf-8') as f:
        for line in tqdm(f, desc="Counting words"):
            tokens = tokenize_odia(line.strip())
            word_counts.update(tokens)

    print(f"  Total unique words: {len(word_counts)}")
    special_tokens = [PAD_TOKEN, UNK_TOKEN, BOS_TOKEN]
    filtered = [(w, c) for w, c in word_counts.items() if c >= MIN_FREQ]
    filtered.sort(key=lambda x: -x[1])
    top_words = [w for w, _ in filtered[:VOCAB_SIZE - len(special_tokens)]]
    all_tokens = special_tokens + top_words
    word2idx = {w: i for i, w in enumerate(all_tokens)}
    idx2word = {i: w for w, i in word2idx.items()}
    print(f"  Vocabulary size: {len(word2idx)}")
    return word2idx, idx2word

def create_sequences(corpus_path: str, word2idx: dict):
    print("Creating training sequences...")
    X_data, y_data = [], []
    unk_idx = word2idx[UNK_TOKEN]
    bos_idx = word2idx[BOS_TOKEN]
    pad_idx = word2idx[PAD_TOKEN]

    with open(corpus_path, 'r', encoding='utf-8') as f:
        for line in tqdm(f, desc="Processing lines"):
            tokens = tokenize_odia(line.strip())
            if len(tokens) < 2:
                continue
            indices = [bos_idx] + [word2idx.get(t, unk_idx) for t in tokens]
            for i in range(1, len(indices)):
                start = max(0, i - SEQUENCE_LEN)
                x = indices[start:i]
                y = indices[i]
                if len(x) < SEQUENCE_LEN:
                    x = [pad_idx] * (SEQUENCE_LEN - len(x)) + x
                X_data.append(x)
                y_data.append(y)

    X = np.array(X_data, dtype=np.int32)
    y = np.array(y_data, dtype=np.int32)
    print(f"  Total samples: {len(X)}")
    return X, y

def main():
    print("="*60)
    print("LSTM DATA PREPARATION")
    print("="*60)

    word2idx, idx2word = build_vocabulary(CORPUS_PATH)
    
    vocab_path = os.path.join(PROCESSED_DIR, "vocab.json")
    with open(vocab_path, 'w', encoding='utf-8') as f:
        json.dump(word2idx, f, ensure_ascii=False, indent=2)
    
    idx_path = os.path.join(PROCESSED_DIR, "idx2word.json")
    with open(idx_path, 'w', encoding='utf-8') as f:
        json.dump(idx2word, f, ensure_ascii=False, indent=2)

    X, y = create_sequences(CORPUS_PATH, word2idx)
    np.save(os.path.join(PROCESSED_DIR, "X_sequences.npy"), X)
    np.save(os.path.join(PROCESSED_DIR, "y_labels.npy"), y)
    
    print(f"\n{'='*60}")
    print("DONE!")
    print(f"   X shape: {X.shape}")
    print(f"   y shape: {y.shape}")
    print(f"   Vocab: {len(word2idx)} words")
    print(f"{'='*60}")

if __name__ == "__main__":
    main()