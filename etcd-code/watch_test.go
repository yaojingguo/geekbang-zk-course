package etcd_code

import (
	"context"
	"fmt"
	v3 "go.etcd.io/etcd/clientv3"
	"testing"
)

var end = []byte("end")

// After start the test, run:
// 	etcdctl put TestOneWatch 1
func TestOneWatch(t *testing.T) {
	cli := newClient(t)
	defer cli.Close()

	rch := cli.Watch(context.Background(), "TestOneWatch")
	for wresp := range rch {
		printEvents(&wresp)
	}
}

// After start the test, run:
// 	etcdctl put TestTwoWatches1 1
// 	etcdctl put TestTwoWatches2 2
func TestTwoWatches(t *testing.T) {
	cli := newClient(t)

	keys := []string{"TestTwoWatches1", "TestTwoWatches2"}
	rch1 := cli.Watch(context.Background(), keys[0])
	rch2 := cli.Watch(context.Background(), keys[1])

	for {
		select {
		case wresp1 := <-rch1:
			printEvents(&wresp1)
		case wresp2 := <-rch2:
			printEvents(&wresp2)
		}
	}
}

func TestWatchFromPast(t *testing.T) {
	cli := newClient(t)
	defer cli.Close()

	key := "TestWatchFromPast"
	ctx := context.Background()

	presp, err := cli.Put(ctx, key, "1")
	if err != nil {
		t.Fatal(err)
	}
	rev := presp.Header.Revision

	rch := cli.Watch(ctx, key, v3.WithRev(rev))
	for wresp := range rch {
		printEvents(&wresp)
	}
	// Output: PUT event TestWatchFromPast: 1
}

func printEvents(resp *v3.WatchResponse) {
	for _, ev := range resp.Events {
		fmt.Printf("%s event key-value %s: %s\n", ev.Type, ev.Kv.Key, ev.Kv.Value)
	}
}
