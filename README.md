# 🧮 Calculatrice Binaire avec UDP et Detection d'erreurs avec Checksum

## 👥 Réalisé par

- Islem Briki  
- Amira El Manaa  
- Aziza Garbâa  

Groupe : RT2/1
Année universitaire : 2024/2025

-------------------------------------------------------------------------------------------------------------------

## 🎯 Objectif

- Appliquer nos connaissances sur les sockets UDP en Java.
- Implémenter une calculatrice binaire.
- Exploiter l'Algorithme de SHUNTING YARD  pour evaluer toute une expression mathematique
- Ajouter une vérification d'intégrité des données à l'aide de la méthode **Checksum**.
- Simuler des erreurs de transmission via un module intermédiaire (simulateur réseau).

------------------------------------------------------------------------------------------------------------------

## 🧩 Architecture du projet

Le système est composé de **trois modules** :

1. **Client (Émetteur)**  
   - Contient l'interface graphique du calculatrice.
   - Accepte les données de l'utilisateur.
   - Calcule le checksum.
   - Envoie les données au simulateur réseau.

2. **Simulateur Réseau (Module intermédiaire)**  
   - Introduit aléatoirement des erreurs (10% de probabilité).
   - Transmet les données modifiées au serveur.

3. **Serveur (Récepteur)**  
   - Vérifie le checksum.
   - Évalue l'expression mathématique (avec l'algorithme **Shunting Yard**).
   - Retourne le résultat ou une erreur au client.

----------------------------------------------------------------------------------------------------------------------

## ⚙️ Fonctionnement

- Communication via **sockets UDP**.
- Représentation binaire des nombres décimaux.
- Calcul des opérations binaires : addition, soustraction, multiplication, division.
- Gestion des erreurs de transmission par recalcul et comparaison du checksum.

---------------------------------------------------------------------------------------------------------------------

## 🖥️ Interface Graphique

- Utilisation de **Swing** en Java.
- Utilisation de **SwingWorker** pour maintenir la réactivité de l’interface.

---------------------------------------------------------------------------------------------------------------------

## 📦 Installation et Exécution

1. **Compilation**
   ```bash
   javac serveur.java
   javac client.java
   javac networksimulator.java
   
2. **Execution**  ( ⚠️ Dans cet ordre )
   ```bash
   java serveur
   java networksimulator
   java client

  
  
   
   
   
