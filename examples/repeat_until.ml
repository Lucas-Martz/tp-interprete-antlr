// Variante asignada: repeat-until.
var n : int = 0;
var suma : int = 0;

repeat {
    print(n);
    suma = suma + n;
    n = n + 1;
} until (n == 5);

if (suma == 10) {
    print("Suma correcta");
} else {
    print("Suma incorrecta");
}
