# Quickstart

Nothing unexpected here:

> sbt compile

> sbt test

> sbt run

# Project structure

This RESTful service is built with `http4s` for HTTP plumbing and `circe` for
JSON processing. Its boilerplate was template-generated with `sbt new
http4s/http4s.g8` in order to leave more time for writing project-specific code.

`Main`:

Application entry point. It's only task is to instantiate `RestfxServer`.

`Restfxserver`:

Instantiates application components (the HTTP server/client and the business
logic `Convert`) and wires them together.

`Restfxroutes`:

Maps API endpoints to business logic.

`Convert`: business logic captured within a tagless final algebra

# Design choices

## Tagless final

It may be debatable if the overhead of the tagless final encoding will pay
itself off. Personally, I don't use it for flexibility in the choice of the
effect type (which is a commonly cited reason) but for parametricity. The less I
know about the effect type, the more disciplined I am with it.

## Protocol messages schema

An unspecified aspect of the presented conversion/quotation protocol is how do
the protocol parties agree on currency codes. I stayed on the "safe" side and
assumed that the currency codes list is pre-shared between all parties and
doesn't change, as opposed to the currency codes occuring in a conversion order
being relayed to the quotation service as strings.

This allows for additional guarantees (messages with "non-existent" currency
codes cannot be constructed or parsed), at the cost of flexibility (conversion
and quotation services will require modification/configuration when a new
currency code is added).

## Scaling to production

Before this RESTful service can be exposed to numerous users, it should be
receiving currency quotes and responding to conversion orders
asynchronously. Asynchronicity can be achieved by caching the quotes and only
updating them once in a while, not every time a conversion order is made. One
reason to do that is to avoid overwhelming the quotes service with
requests. Another reason is to respond to conversion orders faster by reducing
quotes update overhead.

The endpoint shouldn't return a 500 code for a malformed request – JSON parsing
errors should be reported as such. For example, if a "non-existent" currency
code is used, a 400 code should be returned with a clear error message.

# How many tests should be there?

Not too many. I specifically avoided writing tests that would be mostly tests
for the used libraries and tests that could be type signatures.

For example, I trust that `circe` can generate valid JSON from my ADTs, so I'm
not testing that. On the other hand, I don't trust it to derive a parser for a
custom schema without a little help, so there's a test for that. Similarly, I
believe that the HTTP client and server I use work, but I don't trust myself to
write the business logic flawlessly.

It may be debatable if the component wiring can/needs to be tested. I take that
the answer is no. If I'm wiring my components wrong, I'll probably write the
test specification for the wiring wrong. A larger integration test written by
someone else might help to uncover that.
