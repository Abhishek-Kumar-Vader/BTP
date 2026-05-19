import os
import json
import numpy as np
import tensorflow as tf

# Suppress TensorFlow logging to keep the terminal clean
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

def main():
    print("=" * 60)
    print("LSTM NEXT WORD PREDICTION TESTER")
    print("=" * 60)

    processed_dir = "data/processed"
    models_dir = "models/lstm"
    sequence_len = 5

    # 1. Load Vocabulary
    print("Loading vocabulary...")
    try:
        with open(os.path.join(processed_dir, "vocab.json"), 'r', encoding='utf-8') as f:
            word2idx = json.load(f)
        with open(os.path.join(processed_dir, "idx2word.json"), 'r', encoding='utf-8') as f:
            idx2word = json.load(f)
    except FileNotFoundError:
        print("Error: Could not find vocab.json or idx2word.json in data/processed/")
        return

    pad_idx = word2idx.get("<PAD>", 0)
    unk_idx = word2idx.get("<UNK>", 1)
    bos_idx = word2idx.get("<BOS>", 2)

    # 2. Load Model
    model_path = os.path.join(models_dir, "best_model.h5")
    print(f"Loading model from {model_path} (this takes a few seconds)...")
    model = tf.keras.models.load_model(model_path)

    print("\nReady! Type a few Odia words and press Enter to see predictions.")
    print("Type 'exit' to stop.")

    # 3. Interactive Loop
    while True:
        text = input("\nInput context: ").strip()
        
        if text.lower() == 'exit':
            break
        if not text:
            continue

        words = text.split()
        
        # Convert words to indices, prepending the Beginning-of-Sequence token
        indices = [bos_idx] + [word2idx.get(w, unk_idx) for w in words]
        
        # Ensure sequence is exactly 5 tokens long (pad or truncate)
        if len(indices) < sequence_len:
            indices = [pad_idx] * (sequence_len - len(indices)) + indices
        else:
            indices = indices[-sequence_len:]

        x_input = np.array([indices], dtype=np.int32)

        # 4. Predict
        preds = model.predict(x_input, verbose=0)[0]
        
        # Get the top 5 highest probabilities
        top_indices = np.argsort(preds)[-5:][::-1]

        print("Top 5 Predictions:")
        for i, idx in enumerate(top_indices, 1):
            pred_word = idx2word.get(str(idx), "<UNK>")
            prob = preds[idx] * 100
            
            # Skip special padding/unknown tokens in output
            if pred_word not in ["<PAD>", "<UNK>", "<BOS>"]:
                print(f"   {i}. {pred_word} ({prob:.2f}%)")

if __name__ == "__main__":
    main()