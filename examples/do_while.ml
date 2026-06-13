// Variante asignada: do-while.
var n : int = 0;
var suma : int = 0;

do {
    print(n);
    suma = suma + n;
    n = n + 1;
} while (n < 5);

if (suma == 10) {
    print("Suma correcta");
} else {
    print("Suma incorrecta");
}
