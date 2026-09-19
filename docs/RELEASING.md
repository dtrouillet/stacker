# Publier une version

Le dépôt embarque deux workflows GitHub Actions.

| Workflow | Déclencheur | Ce qu'il fait |
|---|---|---|
| `CI` | chaque push et chaque pull request | tests unitaires, Android Lint, APK debug et APK release non signé |
| `Release` | tag `v*`, ou lancement manuel | tests, APK et app bundle signés, release GitHub, envoi aux testeurs |

## 1. Créer la clé de signature

Une fois pour toutes. Cette clé signe toutes les versions publiées : si vous la
perdez, vous ne pourrez plus publier de mise à jour de la même application.
Sauvegardez-la ailleurs que sur GitHub.

```
keytool -genkeypair -v \
  -keystore upload.jks \
  -alias stacker \
  -keyalg RSA -keysize 4096 -validity 10000
```

## 2. Déclarer les secrets du dépôt

Dans **Settings → Secrets and variables → Actions → New repository secret** :

| Secret | Contenu |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | le fichier `upload.jks` encodé en base64 |
| `ANDROID_KEYSTORE_PASSWORD` | le mot de passe du keystore |
| `ANDROID_KEY_ALIAS` | l'alias de la clé, `stacker` ci-dessus |
| `ANDROID_KEY_PASSWORD` | le mot de passe de la clé |

Pour encoder le keystore :

```
base64 -w0 upload.jks     # Linux
base64 upload.jks         # macOS
```

Le workflow s'arrête avec un message explicite si l'un de ces quatre secrets
manque, plutôt que de produire un APK non signé.

## 3. Publier

```
git tag v1.0.0
git push origin v1.0.0
```

Le workflow `Release` construit l'APK et l'app bundle signés, vérifie la
signature avec `apksigner`, puis crée la release GitHub avec les deux fichiers
attachés et des notes générées à partir des commits.

Le numéro de version vient du tag : `v1.2.3` donne `versionName` `1.2.3` et
`versionCode` `10203`. Le workflow refuse un tag qui ne suit pas
`MAJOR.MINOR.PATCH`, et refuse un `minor` ou un `patch` supérieur à 99, qui
casserait la croissance du `versionCode`.

Le fichier `mapping.txt` de R8 est conservé comme artefact du run pendant 90
jours. Il reste hors de la release publique : il ne sert qu'à relire les
rapports de crash.

## 4. Construire sans publier

**Actions → Release → Run workflow** permet de construire une version signée
sans poser de tag. Vous saisissez le numéro de version, et vous choisissez si
les testeurs la reçoivent. Aucune release GitHub n'est créée dans ce cas.

## 5. Distribution aux testeurs, en option

Le workflow envoie l'APK à Firebase App Distribution si les secrets suivants
existent. Sans eux, l'étape est simplement sautée.

| Secret | Contenu |
|---|---|
| `FIREBASE_APP_ID` | l'identifiant de l'app Firebase, de la forme `1:123:android:abc` |
| `FIREBASE_SERVICE_ACCOUNT` | le JSON d'un compte de service ayant le rôle *Firebase App Distribution Admin* |

Et une variable, dans le même écran, onglet **Variables** :

| Variable | Contenu |
|---|---|
| `FIREBASE_GROUPS` | les groupes de testeurs, séparés par des virgules |

## 6. Signer en local

Créez un fichier `keystore.properties` à la racine. Il est déjà dans le
`.gitignore`.

```properties
storeFile=/chemin/vers/upload.jks
storePassword=...
keyAlias=stacker
keyPassword=...
```

Puis :

```
./gradlew assembleRelease -PversionName=1.0.0 -PversionCode=10000
```

Sans ce fichier ni les variables d'environnement, `assembleRelease` fonctionne
quand même et produit un APK non signé.

## Publier sur Google Play

Les workflows s'arrêtent à la release GitHub et aux testeurs Firebase. Pour
aller jusqu'au Play Store, il faut l'app bundle `stacker-x.y.z.aab` joint à la
release, un compte développeur, et une clé de compte de service autorisée sur
l'API Google Play Developer. C'est une étape à ajouter une fois la fiche Play
créée, car elle demande que l'application existe déjà dans la console.
