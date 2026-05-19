import os
import json
import numpy as np
import tensorflow as tf
from tensorflow.keras import layers, callbacks, optimizers
from sklearn.model_selection import train_test_split
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

tf.random.set_seed(42)
np.random.seed(42)

PROCESSED_DIR  = "data/processed"
MODELS_DIR     = "models/lstm"
SEQUENCE_LEN   = 5
EMBEDDING_DIM  = 128
LSTM_UNITS_1   = 256
LSTM_UNITS_2   = 128
DROPOUT_RATE   = 0.3
BATCH_SIZE     = 1024
EPOCHS         = 30
LEARNING_RATE  = 0.001
os.makedirs(MODELS_DIR, exist_ok=True)

def build_model(vocab_size: int, seq_len: int):
    inputs = layers.Input(shape=(seq_len,), name="input_sequence")
    x = layers.Embedding(
        input_dim=vocab_size,
        output_dim=EMBEDDING_DIM,
        mask_zero=True,
        name="embedding"
    )(inputs)
    x = layers.LSTM(
        LSTM_UNITS_1,
        return_sequences=True,
        dropout=DROPOUT_RATE,
        recurrent_dropout=0.1,
        name="lstm_1"
    )(x)
    x = layers.LSTM(
        LSTM_UNITS_2,
        return_sequences=False,
        dropout=DROPOUT_RATE,
        recurrent_dropout=0.1,
        name="lstm_2"
    )(x)
    x = layers.Dropout(DROPOUT_RATE)(x)
    x = layers.Dense(512, activation='relu')(x)
    x = layers.Dropout(0.2)(x)
    outputs = layers.Dense(vocab_size, activation='softmax', name="output")(x)
    model = tf.keras.Model(inputs=inputs, outputs=outputs)
    return model

def plot_history(history, save_path: str):
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5))
    ax1.plot(history.history['loss'], label='Training Loss', linewidth=2)
    ax1.plot(history.history['val_loss'], label='Validation Loss', linewidth=2)
    ax1.set_title('Model Loss')
    ax1.set_xlabel('Epoch')
    ax1.set_ylabel('Loss')
    ax1.legend()
    ax1.grid(True, alpha=0.3)
    ax2.plot(history.history['accuracy'], label='Training Accuracy', linewidth=2)
    ax2.plot(history.history['val_accuracy'], label='Validation Accuracy', linewidth=2)
    ax2.set_title('Model Accuracy')
    ax2.set_xlabel('Epoch')
    ax2.set_ylabel('Accuracy')
    ax2.legend()
    ax2.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig(save_path, dpi=150, bbox_inches='tight')
    print(f"Training plot saved to {save_path}")

def main():
    print("="*60)
    print("LSTM MODEL TRAINING")
    print("="*60)
    
    print("\nLoading data...")
    X = np.load(os.path.join(PROCESSED_DIR, "X_sequences.npy"))
    y = np.load(os.path.join(PROCESSED_DIR, "y_labels.npy"))
    
    # --- NEW FIX: SUBSET THE MASSIVE DATA ---
    MAX_SAMPLES = 5_000_000  # 5 Million samples is the sweet spot for overnight training
    if len(X) > MAX_SAMPLES:
        print(f"\n Dataset is massive ({len(X)}). Subsampling to {MAX_SAMPLES} to save your laptop...")
        indices = np.random.choice(len(X), MAX_SAMPLES, replace=False)
        X = X[indices]
        y = y[indices]
    # ----------------------------------------

    with open(os.path.join(PROCESSED_DIR, "vocab.json"), 'r', encoding='utf-8') as f:
        word2idx = json.load(f)
    VOCAB_SIZE = len(word2idx)
    
    print(f"Vocabulary: {VOCAB_SIZE}")
    print(f"Samples: {len(X)}")

    X_train, X_val, y_train, y_val = train_test_split(
        X, y, test_size=0.1, random_state=42
    )
    print(f"Train: {len(X_train)}, Val: {len(X_val)}")

    print("\nChecking for existing model...")
    model_path = os.path.join(MODELS_DIR, "best_model.h5")
    
    if os.path.exists(model_path):
        print(f"Resuming training from saved model: {model_path}")
        # Make sure tensorflow is imported at the top of your file as tf
        model = tf.keras.models.load_model(model_path)
    else:
        print("No saved model found. Building a new model from scratch...")
        model = build_model(VOCAB_SIZE, SEQUENCE_LEN)
        
    model.summary()

    optimizer = optimizers.Adam(
        learning_rate=LEARNING_RATE,
        clipnorm=1.0
    )
    model.compile(
        optimizer=optimizer,
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )

    checkpoint_path = os.path.join(MODELS_DIR, "best_model.h5")
    model_callbacks = [
        callbacks.ModelCheckpoint(
            checkpoint_path,
            monitor='val_loss',
            save_best_only=True,
            verbose=1
        ),
        callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=3,
            min_lr=1e-6,
            verbose=1
        ),
        callbacks.EarlyStopping(
            monitor='val_loss',
            patience=7,
            restore_best_weights=True,
            verbose=1
        ),
    ]

    print("\nStarting training...")
    print("This will take 1-3 hours depending on your CPU/GPU.")
    print("Press Ctrl+C to stop early if needed.\n")
    
    history = model.fit(
        X_train, y_train,
        validation_data=(X_val, y_val),
        batch_size=BATCH_SIZE,
        epochs=EPOCHS,
        callbacks=model_callbacks,
        verbose=1
    )

    print("\nEvaluating...")
    val_loss, val_acc = model.evaluate(X_val, y_val, verbose=0)
    print(f"Validation Loss: {val_loss:.4f}")
    print(f"Validation Accuracy: {val_acc:.4f}")

    plot_history(history, os.path.join(MODELS_DIR, "training_curves.png"))
    
    print(f"\n{'='*60}")
    print(" TRAINING COMPLETE!")
    print(f"   Best model: {checkpoint_path}")
    print(f"   Plot: models/lstm/training_curves.png")
    print(f"{'='*60}")

if __name__ == "__main__":
    main()