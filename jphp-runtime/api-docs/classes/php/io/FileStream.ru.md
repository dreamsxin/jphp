# FileStream

- **класс** `FileStream` (`php\io\FileStream`) **унаследован от** [`Stream`](https://github.com/jphp-compiler/jphp/blob/master/jphp-runtime/api-docs/classes/php/io/Stream.ru.md)
- **пакет** `std`
- **исходники** `php/io/FileStream.php`

**Описание**

Class FileStream

---

#### Методы

- `->`[`read()`](#method-read)
- `->`[`readFully()`](#method-readfully)
- `->`[`write()`](#method-write)
- `->`[`eof()`](#method-eof)
- `->`[`seek()`](#method-seek)
- `->`[`getPosition()`](#method-getposition)
- `->`[`close()`](#method-close)
- `->`[`length()`](#method-length)
- `->`[`getFilePointer()`](#method-getfilepointer)
- `->`[`truncate()`](#method-truncate)
- См. также в родительском классе [Stream](https://github.com/jphp-compiler/jphp/blob/master/jphp-runtime/api-docs/classes/php/io/Stream.ru.md)

---
# Методы

<a name="method-read"></a>

### read()
```php
read(int $length): mixed
```

---

<a name="method-readfully"></a>

### readFully()
```php
readFully(): mixed
```

---

<a name="method-write"></a>

### write()
```php
write(string $value, null|int $length): int
```

---

<a name="method-eof"></a>

### eof()
```php
eof(): bool
```

---

<a name="method-seek"></a>

### seek()
```php
seek(int $position): mixed
```

---

<a name="method-getposition"></a>

### getPosition()
```php
getPosition(): int
```

---

<a name="method-close"></a>

### close()
```php
close(): mixed
```

---

<a name="method-length"></a>

### length()
```php
length(): int
```

---

<a name="method-getfilepointer"></a>

### getFilePointer()
```php
getFilePointer(): int
```

---

<a name="method-truncate"></a>

### truncate()
```php
truncate(int $size): void
```