# idea-intellij-espend

[![Build Status](https://github.com/Haehnchen/idea-intellij-espend/workflows/Build/badge.svg)](https://github.com/Haehnchen/idea-intellij-espend/actions/workflows/build.yml)

| Key        | Value                     |
|------------|---------------------------|
| Plugin Url | TBA                       |
| ID         | TBA                       |
| Changelog  | [CHANGELOG](CHANGELOG.md) |

## PHP

### Completion

**Given Scope**

```php
namespace App;
class Foobar
{
    /** @var \App\Foobar[] */
    public $cars = [];

    /**  @var string[] */
    public $myIds = [];

    /** @return \App\Foobar[] */
    public function getFoobar() {}
}
```

```php
public function getFoobar()
{
    $items = ['test', 'test2'];
    $ids = [12, 12];
    $prices = [12.12, 12.12];
    $test = new Foobar();
    
    function(__CARET__);
    
    function($scopeLeft, __CARET__);
    function(__CARET__, $scopeRight);
}
```

#### Arrow / Anonymous Function Completion 

**Results**

```php
foo(fn(int $id) => $id, $ids);
foo(fn(string $item) => $item, $items);
foo(fn(Foobar $foobar) => $foobar, $test->getFoobar());
foo(fn(Foobar $foobar) => $foobar, $test->cars);

foo($test->getFoobar(), fn(Foobar $foobar) => $foobar);

foo(static fn(int $id) => $id, $ids);
foo(static fn(string $item) => $item, $items);
foo(static fn(Foobar $foobar) => $foobar, $test->getFoobar());
foo(static fn(Foobar $foobar) => $foobar, $test->cars);

foo(function(int $id) => { return $id;}, $ids)
```

#### WIP: for and foreach

```php
for
```

```php
foreach ($test->getFoobar() as $foobar) {}
for(...) { }
```

### WIP: Parameter Scope Fill

**Given Scope**

```php
function foo(Foobar $foo, int $foo, float $bar);

class Foobar
{
    /** @return \DateTimeInterface[] */
    public function getDate() {}
}

function f()
{
    $test1 = new Foobar();
    $test2 = 1;
    $test3 = 1.3;
    
    foo(__CARET__);
    foo(, __CARET__);
    foo(__CARET__, );
}
```

**Results**

```php
foo($foo->getDate(), $test2, $test3);
```
