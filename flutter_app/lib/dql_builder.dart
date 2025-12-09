import 'package:ditto_live/ditto_live.dart';
import 'package:flutter/material.dart';

class DqlBuilder extends StatefulWidget {
  final Ditto ditto;
  final String query;
  final Map<String, dynamic>? queryArgs;
  final Widget Function(BuildContext, QueryResult) builder;
  final Widget? loading;

  const DqlBuilder({
    super.key,
    required this.ditto,
    required this.query,
    this.queryArgs,
    required this.builder,
    this.loading,
  });

  @override
  State<DqlBuilder> createState() => _DqlBuilderState();
}

class _DqlBuilderState extends State<DqlBuilder> {
  // https://docs.ditto.live/sdk/latest/crud/observing-data-changes
  StoreObserverV2? _observer;

  // https://docs.ditto.live/sdk/latest/sync/syncing-data
  SyncSubscription? _subscription;

  @override
  void initState() {
    super.initState();

    // Register observer, which runs against the local database on this peer
    // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
    final observer = widget.ditto.store.registerObserverV2(
      widget.query,
      arguments: widget.queryArgs ?? {},
    );

    // Register a subscription, which determines what data syncs to this peer
    // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
    final subscription = widget.ditto.sync.registerSubscription(
      widget.query,
      arguments: widget.queryArgs ?? {},
    );

    setState(() {
      _observer = observer;
      _subscription = subscription;
    });
  }

  @override
  void didUpdateWidget(covariant DqlBuilder oldWidget) {
    super.didUpdateWidget(oldWidget);

    final isSame = widget.query == oldWidget.query &&
        widget.queryArgs == oldWidget.queryArgs;

    if (!isSame) {
      _observer?.cancel();
      _subscription?.cancel();

      // Register observer, which runs against the local database on this peer
      // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
      final observer = widget.ditto.store.registerObserverV2(
        widget.query,
        arguments: widget.queryArgs ?? {},
      );

      // Register a subscription, which determines what data syncs to this peer
      // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
      final subscription = widget.ditto.sync.registerSubscription(
        widget.query,
        arguments: widget.queryArgs ?? {},
      );

      setState(() {
        _observer = observer;
        _subscription = subscription;
      });
    }
  }

  @override
  void dispose() {
    _observer?.cancel();
    _subscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final placeholder = widget.loading ?? _defaultLoading;
    final stream = _observer?.changes;
    if (stream == null) return placeholder;

    return StreamBuilder(
        stream: stream,
        builder: (context, snapshot) {
          final response = snapshot.data;
          if (response == null) return widget.loading ?? _defaultLoading;
          return widget.builder(context, response);
        });
  }
}

const _defaultLoading = Center(child: CircularProgressIndicator());
