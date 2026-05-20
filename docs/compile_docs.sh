#!/bin/bash

# S'assurer d'être dans le dossier contenant ce script
cd "$(dirname "$0")"

echo "Nettoyage et création du dossier de sortie 'output'..."
mkdir -p output

# 1. Compilation des fichiers PlantUML (Utilisation du format SVG pour une qualité vectorielle maximale)
echo "Compilation des diagrammes PlantUML en SVG..."
plantuml -tsvg livrable/*.puml

for f in livrable/*.svg; do
    if [ -f "$f" ]; then
        name=$(basename "$f" .svg)
        echo "Conversion vectorielle de ${name} en PDF..."
        # Conversion SVG -> PDF via rsvg-convert pour préserver la qualité vectorielle
        rsvg-convert -f pdf -o "output/${name}.pdf" "$f"
        # Supprimer le fichier SVG temporaire
        rm "$f"
    fi
done

# 2. Compilation des fichiers LaTeX
echo "Compilation du document LaTeX..."
for f in livrable/*.tex; do
    if [ -f "$f" ]; then
        name=$(basename "$f")
        echo "Compilation de ${name} (1/2)..."
        pdflatex -interaction=nonstopmode -output-directory=output "$f" > /dev/null
        
        echo "Compilation de ${name} (2/2) pour les références et la table des matières..."
        pdflatex -interaction=nonstopmode -output-directory=output "$f" > /dev/null
    fi
done

echo "Succès ! Les diagrammes vectoriels et le rapport sont dans 'docs/output'."
