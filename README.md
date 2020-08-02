# INTRODUCTION

Le jeu de Harbour consiste à naviguer des bateaux en mer, éviter les collisions avec les autre bateaux et le rivage, et mener chaque bateau à son port. Il existe différents types de ports (différentes couleurs). Chaque bateau doit être stationner au moins (et au plus) dans un port de même couleur que lui. Pour chaque niveau, il existe un nombre de bateau à stationner. Si le joueur parvient à stationner tous les bateaux du niveau, il a gagné. Sinon, si un ou plusieurs bateaux entrent en collision, c'est la défaite. 
Ce projet consiste à implémenter le jeu de Harbour en Android. Le but de ce projet est d'apprendre la programmation android en codant, se familiariser avec le Map et la géolocalisation, gérer les communications internets...



# ARCHITECTURE DU PROJET

 L'architecture suivi dans tout ce projet est l'architecture MVC (Model-Vue-Controler). 
 - Le Model, dans les fichiers <...>Model.java, contient les données de l'application, ce sont les seules classes autorisées à les manipuler et à les modifier.
 - La Vue, dans les fichier .xml, contient tout ce qui est visible à l'écran et qui propose une interaction avec l'utilisateur.
 - Le Controler, dans les fichiers <...>Activity.java, est l'ensemble des activités, le lien entre les Vue et les Model. Il permet de réagir aux interactions de l'utilisateur et de lui présenter les données qu'il demande (données récupérées dans les Model).
   Dans la suite, nous présentons plus en détail le flux de travail (workflow) suivi pour ce projet.

## MainActivity, l'activité principale

Le premier écran de l'application. Elle contient trois boutons : le bouton Profile qui mène vers la gestion de profile de l'utilisateur, le bouton Statistiques qui mène vers les statistiques du jeu, et le bouton Jeu qui mène vers le jeu. Le background de l'activité est une image du jeu Harbour.
Cette activité est lié à son Model le MainModel. MianModel est une classe qui contient exclusivement les méthodes de sauvegardes et de chargement des données du jeu (l'utilisateur, les stats) et les méthodes de téléchargement de page web (pour télécharger les statistiques et les niveaux de jeu depuis le web). Etant l'activité principale du jeu, l'activité qui lance les autres activités, la MainActivity procède dès son lancement au chargement des données de l'utilisateur, des statistiques de l'utilisateur et aux téléchargements des niveaux du jeu et des autres statistiques depuis le web par son Model. Elle lance la musique de fond par un service et lance ensuite les autres activités lorsque le bouton correspondant est sélectionné.

## ProfileActivity, la gestion de profile

C'est une simple activité contenant une ImageView qui est la photo de profile de l'utilisateur, deux EditText pour afficher et modifier le pseudo et le motto de l'utilisateur et un TextView affichant le meilleur score du joueur. Lorsque l'utilisateur clique sur la photo de profile, l'utilitaire de choix de fichier est lancé, lui permettant ainsi de choisir une nouvelle photo. Une fois les modifications terminées, l'utilisateur peut quitter l'écran et retourner vers l'accueil. Les données modifiées sont automatiquement pris en compte grâce à la surchage de la fonction onBackPressed() dans laquelle on envoie les nouvelles données au MainActivity afin qu'il mette à jour l'utilisateur.

## StatsActivity, la consultation des statistiques

Lorsque l'utilisateur clique sur le bouton Statistiques, le MainActivity lance cette activité en lui passant la liste des statistiques par Intent. Cette liste contient des chaînes de charactères représentant les statistiques chargés par la MainActivity. StatsActivity hérite de ListActivity. Dans le Menu de cette activité, l'utilisateur peut choisir dans quel ordre trier les statistiques en choisissant l'item correspondant. La liste est aussitôt triées suivant cet ordre. Dans l'affichange, les statistiques chargés du web sont précédés d'un astérix (\*) pour permettre à l'utilisateur de les différencier de ses propres statistiques.

## GameActivity, les activités du jeu

Le jeu en lui même contient deux écrans. Lorsque l'utilisateur clique sur le bouton Jeu, la MainActivity envoie par Intent les différents niveaux chargés du web, les infos du joueur et les statistiques à la première activité du jeu, la MapsActivity.

### MapsActivity, choix du niveau

C'est une activité qui contient la Map du monde et des Marker sur certaines localisation où le jeu est possible. La localisation et les noms de ces Marker sont téléchargés sur le web par la MainActivity. Chaque Marker représente un niveau du jeu avec un nombre de bateau à parker différent. Lorsque l'utilisateur sélectionne un Marker, un AlertDialog s'ouvre lui présentant les infos sur le niveau (le nom du niveau, le niveau de difficulté et le nombre de bateau à parker). Si l'utilisateur décide de continuer, on met la musique de fond du jeu et le second écran du jeu est alors lancé. La MapsActivity lui passe par Intent l'utilisateur, le nom du niveau, la difficulté du niveau, la difficulté maximale du jeu et le nombre de bateau à parker.

### GameActivity, le jeu en soi

C'est l'activité la plus importante du jeu. Elle contient le jeu en lui même. Elle est liée à son propre Model, le GameModel et contient plusieurs classes internes.
			
#### GameModel et ses différentes fonctions

C'est le Model du Jeu. Il est totalement indépendant dans la vue. Il contient toutes les données du jeu et les fonctions permettant de les manipuler.

**'Boat'** : La classe Boat est une classe interne à GameModel. C'est la classe qui défini les bateaux. Un bateau est défini par sa position précédente (PointF prec), sa position courante (PointF cur), sa couleur, une liste des prochains points à suivre (LinkedList<>PointF path) et un boolean permettant de savoir s'il est encore dans la mer ou non (isOn). A l'initialisation, isOn est toujours à true.

**Les Attributs** : GameModel contient les coordonnées et la taille du rivage, les coordonnées des ports et leurs couleurs, un tableau de bateau qui représente tous les bateaux de ce niveau, etc... Le tableau des bateaux est seulement alloué dans le constructeur. A chaque fois qu'un nouveau bateau est envoyé dans la mer, sa case dans le tableau est initialisé.

**'newBoat'** : envoie un nouveau bateau dans la mer. On choisit aléatoirement le côté et la position où apparaîtra le bateau. Puis on initialise la case courante du tableau des bateaux au nouveau bateau.

**'moveBoat'** : déplace tous les bateaux en mer (en vérifiant l'attribut isOn). Le déplacement se fait comme suit : si un path a été défini par l'utilisateur pour ce bateau (path est non vide), alors on poll le premier point de path et on set la position du bateau à ce pont. Sinon, la nouvelle position du bateau est égale à la position courante + la position courante moins la position précédente le tout modulo la taille de la Vue.

**'delivery'** : teste si un bateau a été parké. On teste si la position du bateau est dans l'un des quays de la même couleur que lui. Pour cela, on utilise la méthode RectF.contains.

**'collision'** : teste s'il y a collision pour l'un des bateaux ou non. Dans un premier temps, on teste, pour tous les bateaux en mer, si deux d'entre eux s'intersecte. Pour cela, on utilise la méthode RectF.intersect car les bateaux sont considérés comme des rectangle. Dans un second temps, on teste si l'un des bateaux en mer entre en collision avec le rivage. Pour cela, on teste si la postion du bateaux appartient au cercle qui représente le rivage.

**'boatSelected'** : avec les coordonnées X et Y d'un point, vérifie si ces coordonnées coincide avec celles de l'un des bateaux en mer. Dans ce cas, le bateau en question est sélectionné par l'utilisateur.

**'addPath'** : si un bateau a été sélectionné, ajoute les coordonnées au chemin à suivre par le bateau.

#### Les fonctionnalités du GameActivity

**'GroundLevel1'** : C'est une classe interne à GameActivity qui hérite de View et déssine le terrain du jeu. Les coordonnées des ports et des quays de rivage sont définis dans GameModel. GroundLevel1 détecte aussi les actions de l'utilisateur sur l'écran, permettant ainsi de définir des chemins à suivre pour les bateaux. Lorsque ACTION_DOWN puis ACTION_MOVE est détecté, on construit un path et on ajoute les coordonnées X et Y à la liste path du bateau sélectionné.

Pour toutes les fonctionnalités du jeu, on utilise la méthode Timer.scheduleAtFixedRate. Ainsi, la GameActivity contient six handler : timeHandler pour le chronométrage et son affichage, launchHandler pour lancer de nouveaux bateaux en mer, moveHandler pour déplacer les bateaux, parkHandler pour garer les bateaux correctement dans les quays, remHandler pour lancer le mécanisme de suppression des bateaux de la vue et endHandler pour terminer le jeu. GameActivity contient son propre tableau des bateaux qui est ici un tableau de ImageView. Ce tableau a la même taille que celui du model net ces éléments sont localisés exactement à la même position que leur correspondant dans le model. Voici maintenant le déroulement globale du jeu :
après avoir initialisé tous ses attributs, la GameActivity lance un thread pour s'occuper du jeu (GameThread). Ce thread crée trois TimerTask : le TimerTask pour le chrono qui a une période d'une seconde, celui pour le déplacement des bateaux qui a une période 50 millisecondes et celui pour envoyer de nouveaux bateaux dans la mer qui a une période de 5 secondes. On lance un premier bateau dans la mer, chaque 50 millisecondes on demande au GameModel de déplacer les bateaux, on met à jour ensuite les nouvelles positions dans la vue. Une fois les déplacements fait, on vérifie si l'un des bateaux a été parké. Si oui, on lance la procédure de parkage puis de suppression du bateau. La procédure de suppression est un nouveau thread qui laisse le bateau garé dans le quay pendant 5 s puis le supprime. On vérifie ensuite si tous les bateaux ont été parkés. Si c'est le cas, le jeu est fini, le joueur a gagné. On lance la procédure de fin. Si aucun bateau n'a été parké, on vérifie s'il y a collision. Si oui, on arrête, le joueur a perdu. On lance la procédure de fin. La procédure de fin consiste à afficher un AlertDialog présentant les statistiques du joueur pour ce jeu. Le joueur est ensuite redirigé vers la MapsActivity. Notez que toutes les procédures de vérifications sont faites par le GameModel. Chaque 5 secondes, seulement si tous les bateaux n'ont pas encore été envoyé en mer et que le nombre de bateaux actuellement en mer ne dépasse pas le nombre de bateaux maximale à être en mer au même moment, on demande au model d'envoyer un nouveau bateau en mer puis on crée le bateau correspondant dans la vue.

A la fin de cette activité, on envoie la nouvelle statistique du joueur et le meilleur score au MapsActivity qui fait les mise à jour.


# LES OPTIONS

Nous avons implémenté les options suivants :
**Persistance des données** : lorsque l'utilisateur quitte le jeu, on sauvegarde les données utilisateur (le User, qui est sérialisable) dans un fichier user.txt et les statistiques (dans stats.txt) permettant ainsi de ne perdre aucune information du jeu (voir MainModel.java).

**Tri des résultats** : Comme expliqué plus haut, nous trions les résultats selon toutes les composantes disponibles (voir ProfileActivity.java).


# DIFFICULTES RENCONTREES

La principale difficulté rencontrée tout au long du projet est la non maîtrise de la bibliothèque Android. Pour plusieurs fonctions, il nous est arrivé de chercher sur internet comment les utiliser. Outre ce problème, j'ai aussi fait face par moment à d'autres petits soucis. Par exemple, j'avais pensé à utiliser un TableView pour les statistiques au lieu d'une ListView facilitant ainsi les tris et l'affichage. Cependant, c'est avec du regret que je me suis rendu compte que cet objet n'existait pas en Android, au contraire de JavaFX. Il fallait l'implémenter soi-même. Après plusieurs heures et plusieurs tentatives sans succès, j'ai abandonné. Un autre soucis rencontré était aussi la gestion des collisions. Trouver la méthode qui détecte avec exactitude les interssections entre les rectangles et les interssections entre rectangle et cercle ne fût pas aisé. Mais dans sa globalité, le projet était intéressant et riche d'apprentissage. Il était certes long, mais je me suis bien plut à travailler dessus.

# Auteur
[BAMBA Ibrahim](https://www.linkedin.com/in/ibrahim-bamba-885a05115)
ikader737@gmail.com