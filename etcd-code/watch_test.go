package etcd_code

import (
	"context"
	"fmt"
	v3 "go.etcd.io/etcd/client/v3"
	"testing"
)

var end = []byte("end")

// After start the test, run:
// 	etcdctl put TestOneWatch 1
func TestOneWatch(t *testing.T) {
	cli := newClient(t)
	defer cli.Close()
	ctx, cancel := context.WithCancel(context.Background())

	rch := cli.Watch(ctx, "TestOneWatch")
	for wresp := range rch {
		printEvents(&wresp)
		cancel()
	}
}

// After start the test, run:
// 	etcdctl put TestTwoWatches1 1
// 	etcdctl put TestTwoWatches2 2
func TestTwoWatches(t *testing.T) {
	cli := newClient(t)
	ctx, cancel := context.WithCancel(context.Background())

	keys := []string{"TestTwoWatches1", "TestTwoWatches2"}
	rch1 := cli.Watch(ctx, keys[0])
	rch2 := cli.Watch(ctx, keys[1])

	count := 0
	for {
		select {
		case wresp1 := <-rch1:
			printEvents(&wresp1)
			count += 1
		case wresp2 := <-rch2:
			printEvents(&wresp2)
			count += 1
		}
		if count == 2{
			cancel()
			break
		}
	}
}

func TestWatchPast(t *testing.T) {
	cli := newClient(t)
	defer cli.Close()

	key := "TestWatchPast"
	ctx, cancel := context.WithCancel(context.Background())

	presp, err := cli.Put(ctx, key, "1")
	if err != nil {
		t.Fatal(err)
	}
	rev := presp.Header.Revision

	rch := cli.Watch(ctx, key, v3.WithRev(rev))
	for wresp := range rch {
		printEvents(&wresp)
		cancel()
	}
	// Watch can watch events happened in the past.
	// Output: PUT event TestWatchFromPast: 1
}

func printEvents(resp *v3.WatchResponse) {
	for _, ev := range resp.Events {
		fmt.Printf("%s event key-value: %s => %s\n", ev.Type, ev.Kv.Key, ev.Kv.Value)
	}
}
