import os
import json
import re
from collections import Counter, defaultdict
from tqdm import tqdm

CORPUS_PATH = "data/raw/combined_corpus.txt"
MODELS_DIR   = "models/spell_correction"
EXPORT_DIR   = "android_export"
MIN_WORD_FREQ = 3
os.makedirs(MODELS_DIR, exist_ok=True)
os.makedirs(EXPORT_DIR, exist_ok=True)

def tokenize(text):
    text = re.sub(r'[^\u0B00-\u0B7F\s]', ' ', text)
    return [t for t in text.split() if t.strip()]

def main():
    print("="*60)
    print("N-GRAM MODEL BUILDING")
    print("="*60)
    
    print("\nCounting words and N-grams...")
    unigrams = Counter()
    bigrams = defaultdict(Counter)
    word_dict = Counter()
    
    with open(CORPUS_PATH, 'r', encoding='utf-8') as f:
        for line in tqdm(f, desc="Processing"):
            words = tokenize(line.strip())
            if not words:
                continue
            word_dict.update(words)
            padded = ["<BOS>", "<BOS>"] + words + ["<EOS>"]
            for i, w in enumerate(padded):
                unigrams[w] += 1
                if i > 0:
                    bigrams[padded[i-1]][w] += 1
    
    total = sum(unigrams.values())
    valid_words = {w: c for w, c in word_dict.items() if c >= MIN_WORD_FREQ and not w.startswith('<')}
    
    print(f"Dictionary: {len(valid_words)} words")
    
    # Save compact bigrams for Android
    compact_bigrams = {}
    for prev, nexts in bigrams.items():
        if prev.startswith('<') or prev not in valid_words:
            continue
        top = sorted(nexts.items(), key=lambda x: -x[1])[:20]
        top_valid = [[w, c] for w, c in top if w in valid_words]
        if top_valid:
            compact_bigrams[prev] = top_valid
    
    import pickle
    with open(os.path.join(MODELS_DIR, "ngram_model.pkl"), 'wb') as f:
        pickle.dump({
            "unigrams": dict(unigrams),
            "bigrams": dict(bigrams),
            "valid_words": valid_words,
            "total": total
        }, f)
    
    android_data = {"words": valid_words, "bigrams": compact_bigrams}
    with open(os.path.join(EXPORT_DIR, "ngram_model.json"), 'w', encoding='utf-8') as f:
        json.dump(android_data, f, ensure_ascii=False, separators=(',', ':'))
    
    with open(os.path.join(EXPORT_DIR, "odia_dictionary.json"), 'w', encoding='utf-8') as f:
        json.dump(list(valid_words.keys()), f, ensure_ascii=False, separators=(',', ':'))
    
    print(f"\n{'='*60}")
    print(" N-gram model saved!")
    print(f"{'='*60}")

if __name__ == "__main__":
    main()