import os
import json
import numpy as np
import tensorflow as tf
import shutil

MODELS_DIR    = "models/lstm"
PROCESSED_DIR = "data/processed"
EXPORT_DIR    = "android_export"
os.makedirs(EXPORT_DIR, exist_ok=True)

def main():
    print("="*60)
    print("EXPORTING LSTM MODEL FOR ANDROID")
    print("="*60)

    keras_path = os.path.join(MODELS_DIR, "best_model.h5")
    tflite_path = os.path.join(EXPORT_DIR, "lstm_model.tflite")

    print(f"Loading best model from {keras_path}...")
    model = tf.keras.models.load_model(keras_path)

    print("Converting to TFLite and compressing...")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    
    # --- NEW FIX FOR LSTM CONVERSION ---
    # Allow TFLite to use advanced TensorFlow operations for the LSTM loops
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS, 
        tf.lite.OpsSet.SELECT_TF_OPS
    ]
    converter._experimental_lower_tensor_list_ops = False
    # -----------------------------------
    
    # This optimization reduces the file size by roughly 75%
    converter.optimizations = [tf.lite.Optimize.DEFAULT] 
    
    tflite_model = converter.convert()

    with open(tflite_path, 'wb') as f:
        f.write(tflite_model)

    size_kb = os.path.getsize(tflite_path) / 1024
    print(f"TFLite model saved: {tflite_path} ({size_kb:.1f} KB)")

    print("\nCopying vocabulary files to export folder...")
    for fname in ["vocab.json", "idx2word.json"]:
        src = os.path.join(PROCESSED_DIR, fname)
        dst = os.path.join(EXPORT_DIR, fname)
        shutil.copy(src, dst)
        print(f"  -> Copied {fname}")

    print(f"\n{'='*60}")
    print(" EXPORT COMPLETE! Your model is ready for mobile.")
    print(f"{'='*60}")

if __name__ == "__main__":
    main()