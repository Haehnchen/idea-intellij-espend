<?php

namespace
{
    function foobar(\App\Foobar $foobar, int $foo, \App\Foobar $foobar1, int $foobar2) {}
}

namespace App
{
    class NotMyFoobar {}

    class Foobar {}

    class FoobarFactory
    {

        public function __construct(public Foobar $fooProperty)
        {
        }

        /**
         * @return Foobar
         */
        public function createFoobar() {}

        /**
         * @return NotMyFoobar
         */
        public function getNotMyFoobar() {}
    }
}